package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.fsdm.client.services.IActiveFlagService;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.ISecurityTokenService;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.activeflag.IActiveFlag;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.abstraction.WarehouseCoreTable;
import com.guicedee.activitymaster.fsdm.db.abstraction.WarehouseSCDTable;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.fsdm.db.entities.geography.GeographyXClassification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.GeographyXGeography;
import com.guicedee.activitymaster.geography.services.dto.GeoNameDefaultData;
import com.guicedee.activitymaster.geography.services.dto.GeographyPostalCode;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.IntConsumer;

import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.NoClassification;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts.NoClassificationDataConceptName;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

/** Ordered bounded inserts using only the caller's stateless session and transaction. */
final class GeographyBulkWriter {
    static final int BATCH_SIZE = 128;
    private final Map<GeographyClassifications, Classification> attributes = new EnumMap<>(GeographyClassifications.class);
    private Classification hierarchyType;
    private IActiveFlag<?, ?> active;
    private Map<String, com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.security.ISecurityToken<?, ?>> tokens;

    static Uni<GeographyBulkWriter> resolve(Mutiny.StatelessSession session, ISystems<?, ?> system,
            IClassificationService<?> classifications, IActiveFlagService<?> flags,
            ISecurityTokenService<?> securities, UUID... identity) {
        var writer = new GeographyBulkWriter();
        Uni<Void> chain = Uni.createFrom().voidItem();
        // GeographySystemInstall uses the enum/parent create overload, which stores these
        // taxonomy nodes in NoClassificationDataConceptName. Resolve that installed concept.
        for (var key : List.of(Town, City, PostalCode, PostalCodeSuburb, GeoNameID, Latitude, Longitude, Population, Elevation, DEM))
            chain = chain.chain(() -> classifications.find(session, key.toString(), NoClassificationDataConceptName, system, identity)
                    .invoke(c -> writer.attributes.put(key, (Classification) c)).replaceWithVoid());
        return chain.chain(() -> classifications.find(session, NoClassification.toString(), NoClassificationDataConceptName, system, identity))
                .invoke(c -> writer.hierarchyType = (Classification) c)
                .chain(() -> flags.getActiveFlag(session, system.getEnterprise(), identity))
                .invoke(a -> writer.active = a)
                .chain(() -> securities.resolveDefaultGroupFolderTokens(session, system, identity))
                .invoke(t -> writer.tokens = t).replaceWith(writer);
    }

    Uni<Void> load(Mutiny.StatelessSession session, ISystems<?, ?> system,
            Map<Long, GeoNameDefaultData<?>> data, Map<Long, List<Long>> hierarchy, IntConsumer total, IntConsumer progress) {
        OffsetDateTime now = OffsetDateTime.now();
        // Scalar reads avoid stateless hydration of cacheable relationship entities.
        return session.createQuery("""
                select l.value, l.geographyID.id from GeographyXClassification l
                where l.enterpriseID.id = :enterprise and l.classificationID.id = :type
                  and l.geographyID.classificationID.id = :district
                  and l.effectiveFromDate <= :now and l.effectiveToDate >= :now
                """, Object[].class)
                .setParameter("enterprise", system.getEnterprise().getId())
                .setParameter("type", attributes.get(GeoNameID).getId())
                .setParameter("district", attributes.get(City).getId()).setParameter("now", now)
                .getResultList().chain(rows -> {
                    Map<Long, Geography> parents = new LinkedHashMap<>();
                    for (Object[] row : rows) {
                        if (row[0] == null || row[0].toString().isBlank()) continue;
                        long source = Long.parseLong(row[0].toString());
                        if (data.containsKey(source) && hierarchy.containsKey(source))
                            parents.put(source, new Geography().setId((UUID) row[1]));
                    }
                    List<Place> places = new ArrayList<>();
                    for (var parent : parents.entrySet())
                        for (Long child : hierarchy.get(parent.getKey()))
                            if (data.containsKey(child)) places.add(new Place(parent.getValue(), data.get(child)));
                    total.accept(places.size());
                    Uni<Void> chain = Uni.createFrom().voidItem();
                    for (int from = 0; from < places.size(); from += BATCH_SIZE) {
                        List<Place> chunk = List.copyOf(places.subList(from, Math.min(from + BATCH_SIZE, places.size())));
                        chain = chain.chain(() -> writeChunk(session, system, chunk)).invoke(() -> progress.accept(chunk.size()));
                    }
                    return chain;
                });
    }

    private record Place(Geography parent, GeoNameDefaultData<?> data) {}

    Uni<Void> loadPostal(Mutiny.StatelessSession session, ISystems<?, ?> system,
            Map<Long, List<GeographyPostalCode>> data, IntConsumer total, IntConsumer progress) {
        total.accept(data.size());
        List<List<GeographyPostalCode>> groups = data.values().stream().filter(g -> !g.isEmpty()).toList();
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (int from = 0; from < groups.size(); from += BATCH_SIZE) {
            var chunk = List.copyOf(groups.subList(from, Math.min(from + BATCH_SIZE, groups.size())));
            chain = chain.chain(() -> writePostalChunk(session, system, chunk)).invoke(() -> progress.accept(chunk.size()));
        }
        return chain;
    }

    private record PostalIdentity(UUID type, String name, String description) {}

    private Uni<Void> writePostalChunk(Mutiny.StatelessSession session, ISystems<?, ?> system,
            List<List<GeographyPostalCode>> groups) {
        OffsetDateTime now = OffsetDateTime.now();
        List<String> names = groups.stream().map(g -> g.getFirst().getParentPlaceName()).distinct().toList();
        List<String> codes = groups.stream().map(g -> PostalCodeService.formatCode(g.getFirst().getPostalCode())).distinct().toList();
        List<String> provinces = groups.stream().map(g -> g.getFirst().getProvinceCode()).filter(Objects::nonNull).distinct().toList();
        return session.createQuery("""
                select g.name, g.id from Geography g
                where g.enterpriseID.id = :enterprise and g.classificationID.id = :town
                  and g.name in :names and g.activeFlagID.id = :active
                  and g.effectiveFromDate <= :now and g.effectiveToDate >= :now
                order by g.id
                """, Object[].class).setParameter("enterprise", system.getEnterprise().getId())
                .setParameter("town", attributes.get(Town).getId()).setParameter("names", names)
                .setParameter("active", active.getId()).setParameter("now", now).getResultList().chain(townRows -> {
                    Map<String, Geography> towns = new HashMap<>();
                    for (Object[] row : townRows) towns.putIfAbsent((String) row[0], new Geography().setId((UUID) row[1]));
                    return session.createQuery("""
                            select l.parentGeographyID.name, l.childGeographyID.id from GeographyXGeography l
                            where l.enterpriseID.id = :enterprise and l.classificationID.id = :type
                              and l.parentGeographyID.name in :provinces
                              and l.childGeographyID.classificationID.id = :district
                              and l.activeFlagID.id = :active
                              and l.childGeographyID.activeFlagID.id = :active
                              and l.childGeographyID.effectiveFromDate <= :now and l.childGeographyID.effectiveToDate >= :now
                              and l.effectiveFromDate <= :now and l.effectiveToDate >= :now
                            order by l.childGeographyID.id
                            """, Object[].class).setParameter("enterprise", system.getEnterprise().getId())
                            .setParameter("type", hierarchyType.getId()).setParameter("provinces", provinces)
                            .setParameter("district", attributes.get(City).getId())
                            .setParameter("active", active.getId()).setParameter("now", now).getResultList().chain(districtRows -> {
                                Map<String, Geography> districts = new HashMap<>();
                                for (Object[] row : districtRows) districts.putIfAbsent((String) row[0], new Geography().setId((UUID) row[1]));
                                return session.createQuery("""
                                        select g.classificationID.id, g.name, g.description, g.id from Geography g
                                        where g.enterpriseID.id = :enterprise and g.classificationID.id in :types
                                          and g.name in :codes and g.activeFlagID.id = :active
                                          and g.effectiveFromDate <= :now and g.effectiveToDate >= :now
                                        """, Object[].class).setParameter("enterprise", system.getEnterprise().getId())
                                        .setParameter("types", List.of(attributes.get(PostalCode).getId(), attributes.get(PostalCodeSuburb).getId()))
                                        .setParameter("codes", codes).setParameter("active", active.getId()).setParameter("now", now)
                                        .getResultList().chain(existingRows -> {
                                            Map<PostalIdentity, Geography> existing = new HashMap<>();
                                            for (Object[] row : existingRows) {
                                                String description = row[0].equals(attributes.get(PostalCode).getId()) ? null : (String) row[2];
                                                Geography previous = existing.putIfAbsent(new PostalIdentity((UUID) row[0], (String) row[1], description), new Geography().setId((UUID) row[3]));
                                                if (previous != null && !previous.getId().equals(row[3]))
                                                    throw new IllegalStateException("Multiple current geography rows for the same postal identity");
                                            }
                                            List<Geography> geographies = new ArrayList<>();
                                            List<GeographyXClassification> classifications = new ArrayList<>();
                                            Set<List<UUID>> required = new LinkedHashSet<>();
                                            for (var group : groups) {
                                                var first = group.getFirst();
                                                Geography town = towns.get(first.getParentPlaceName());
                                                if (town == null) {
                                                    Geography district = districts.get(first.getProvinceCode());
                                                    if (district == null) continue; // Same unsupported-province behavior as the single-row loader.
                                                    town = postalRow(system, Town, first.getParentPlaceName(), first.getParentPlaceName(), "", geographies, classifications);
                                                    towns.put(first.getParentPlaceName(), town);
                                                    required.add(List.of(district.getId(), town.getId()));
                                                }
                                                String code = PostalCodeService.formatCode(first.getPostalCode());
                                                var key = new PostalIdentity(attributes.get(PostalCode).getId(), code, null);
                                                Geography postal = existing.get(key);
                                                if (postal == null) {
                                                    postal = postalRow(system, PostalCode, code, first.getPostalCodePlaceName(), "", geographies, classifications);
                                                    existing.put(key, postal);
                                                }
                                                required.add(List.of(town.getId(), postal.getId()));
                                                for (var suburb : group) {
                                                    String suburbCode = PostalCodeService.formatCode(suburb.getPostalCode());
                                                    var suburbKey = new PostalIdentity(attributes.get(PostalCodeSuburb).getId(), suburbCode, suburb.getPostalCodePlaceName());
                                                    Geography child = existing.get(suburbKey);
                                                    if (child == null) {
                                                        child = postalRow(system, PostalCodeSuburb, suburbCode, suburb.getPostalCodePlaceName(), suburb.getPostalCode(), geographies, classifications);
                                                        existing.put(suburbKey, child);
                                                    }
                                                    suburb.setGeographyId(child.getId());
                                                    required.add(List.of(postal.getId(), child.getId()));
                                                }
                                            }
                                            if (required.isEmpty()) return Uni.createFrom().voidItem();
                                            return session.createQuery("""
                                                    select l.parentGeographyID.id, l.childGeographyID.id from GeographyXGeography l
                                                    where l.enterpriseID.id = :enterprise and l.classificationID.id = :type
                                                      and l.childGeographyID.id in :children and l.activeFlagID.id = :active
                                                      and l.effectiveFromDate <= :now and l.effectiveToDate >= :now
                                                    """, Object[].class).setParameter("enterprise", system.getEnterprise().getId())
                                                    .setParameter("type", hierarchyType.getId())
                                                    .setParameter("children", required.stream().map(p -> p.get(1)).distinct().toList())
                                                    .setParameter("active", active.getId()).setParameter("now", now).getResultList().chain(existingLinks -> {
                                                        for (Object[] link : existingLinks) required.remove(List.of((UUID) link[0], (UUID) link[1]));
                                                        List<GeographyXGeography> links = new ArrayList<>();
                                                        for (var pair : required) {
                                                            var link = new GeographyXGeography(); configure(link, system);
                                                            link.setParentGeographyID(new Geography().setId(pair.get(0)));
                                                            link.setChildGeographyID(new Geography().setId(pair.get(1)));
                                                            link.setClassificationID(hierarchyType); link.setValue(""); links.add(link);
                                                        }
                                                        return persistRows(session, system, geographies, classifications, links);
                                                    });
                                        });
                            });
                });
    }

    private Geography postalRow(ISystems<?, ?> system, GeographyClassifications type, String name,
            String description, String source, List<Geography> rows, List<GeographyXClassification> classifications) {
        var row = new Geography(); configure(row, system);
        row.setName(name); row.setDescription(description); row.setClassificationID(attributes.get(type));
        rows.add(row); attribute(classifications, row, GeoNameID, source, system);
        return row;
    }

    private Uni<Void> persistRows(Mutiny.StatelessSession session, ISystems<?, ?> system,
            List<Geography> geographies, List<GeographyXClassification> classifications, List<GeographyXGeography> hierarchies) {
        List<WarehouseCoreTable<?, ?, ?, ?>> rows = new ArrayList<>();
        rows.addAll(geographies); rows.addAll(classifications); rows.addAll(hierarchies);
        List<Object> security = new ArrayList<>();
        for (var row : rows) security.addAll(row.prepareDefaultSecurityRows(system, system.getEnterprise(), active, tokens));
        return insert(session, geographies).chain(() -> insert(session, classifications))
                .chain(() -> insert(session, hierarchies)).chain(() -> insert(session, security));
    }

    private Uni<Void> writeChunk(Mutiny.StatelessSession session, ISystems<?, ?> system, List<Place> places) {
        List<String> sources = places.stream().map(p -> p.data.getGeonameId().toString()).distinct().toList();
        OffsetDateTime now = OffsetDateTime.now();
        return session.createQuery("""
                select l.value, l.geographyID.id from GeographyXClassification l
                where l.enterpriseID.id = :enterprise and l.classificationID.id = :type
                  and l.value in :sources and l.effectiveFromDate <= :now and l.effectiveToDate >= :now
                """, Object[].class).setParameter("enterprise", system.getEnterprise().getId())
                .setParameter("type", attributes.get(GeoNameID).getId()).setParameter("sources", sources)
                .setParameter("now", now).getResultList().chain(existing -> {
                    Map<String, Geography> ids = new HashMap<>();
                    for (Object[] row : existing) ids.put(row[0].toString(), new Geography().setId((UUID) row[1]));
                    List<Geography> geographies = new ArrayList<>();
                    List<GeographyXClassification> classifications = new ArrayList<>();
                    for (Place place : places) {
                        String source = place.data.getGeonameId().toString();
                        Geography geo = ids.get(source);
                        if (geo == null) {
                            geo = new Geography();
                            configure(geo, system);
                            geo.setName(place.data.getName());
                            geo.setDescription(place.data.getAsciiname());
                            geo.setClassificationID(attributes.get(Town));
                            ids.put(source, geo);
                            geographies.add(geo);
                            attribute(classifications, geo, GeoNameID, source, system);
                            if (place.data.getCoordinates() != null) {
                                attribute(classifications, geo, Latitude, place.data.getCoordinates().getLatitude(), system);
                                attribute(classifications, geo, Longitude, place.data.getCoordinates().getLongitude(), system);
                            }
                            if (place.data.getPopulation() != 0) attribute(classifications, geo, Population, Integer.toString(place.data.getPopulation()), system);
                            if (place.data.getElevation() != 0) attribute(classifications, geo, Elevation, Integer.toString(place.data.getElevation()), system);
                            if (place.data.getDem() != 0) attribute(classifications, geo, DEM, Integer.toString(place.data.getDem()), system);
                        }
                        place.data.setGeographyId(geo.getId());
                    }
                    return session.createQuery("""
                            select l.parentGeographyID.id, l.childGeographyID.id from GeographyXGeography l
                            where l.enterpriseID.id = :enterprise and l.classificationID.id = :type
                              and l.childGeographyID.id in :children
                              and l.effectiveFromDate <= :now and l.effectiveToDate >= :now
                            """, Object[].class).setParameter("enterprise", system.getEnterprise().getId())
                            .setParameter("type", hierarchyType.getId())
                            .setParameter("children", ids.values().stream().map(Geography::getId).distinct().toList())
                            .setParameter("now", now).getResultList().chain(links -> {
                                Set<List<UUID>> linked = new HashSet<>();
                                for (Object[] link : links) linked.add(List.of((UUID) link[0], (UUID) link[1]));
                                List<GeographyXGeography> hierarchies = new ArrayList<>();
                                for (Place place : places) {
                                    Geography child = ids.get(place.data.getGeonameId().toString());
                                    if (!linked.add(List.of(place.parent.getId(), child.getId()))) continue;
                                    var link = new GeographyXGeography();
                                    configure(link, system);
                                    link.setParentGeographyID(place.parent);
                                    link.setChildGeographyID(child);
                                    link.setClassificationID(hierarchyType);
                                    link.setValue("");
                                    hierarchies.add(link);
                                }
                                List<WarehouseCoreTable<?, ?, ?, ?>> newRows = new ArrayList<>();
                                newRows.addAll(geographies); newRows.addAll(classifications); newRows.addAll(hierarchies);
                                List<Object> securityRows = new ArrayList<>();
                                for (var row : newRows)
                                    securityRows.addAll(row.prepareDefaultSecurityRows(system, system.getEnterprise(), active, tokens));
                                return insert(session, geographies).chain(() -> insert(session, classifications))
                                        .chain(() -> insert(session, hierarchies)).chain(() -> insert(session, securityRows));
                            });
                });
    }

    private void attribute(List<GeographyXClassification> rows, Geography geo,
            GeographyClassifications key, String value, ISystems<?, ?> system) {
        if (value != null && value.length() > 254) throw new IllegalArgumentException("Geography attribute exceeds 254 characters: " + key);
        var row = new GeographyXClassification();
        configure(row, system);
        row.setGeographyID(geo); row.setClassificationID(attributes.get(key)); row.setValue(value);
        rows.add(row);
    }

    private void configure(WarehouseSCDTable<?, ?, ?, ?> row, ISystems<?, ?> system) {
        row.setEnterpriseID(system.getEnterprise()); row.setSystemID(system);
        row.setActiveFlagID(active); row.setOriginalSourceSystemID(system.getId());
        row.setOriginalSourceSystemUniqueID(new UUID(0, 0));
    }

    private static Uni<Void> insert(Mutiny.StatelessSession session, List<?> rows) {
        Map<Class<?>, List<Object>> byType = new LinkedHashMap<>();
        for (Object row : rows) byType.computeIfAbsent(row.getClass(), ignored -> new ArrayList<>()).add(row);
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (List<Object> group : byType.values())
            for (int from = 0; from < group.size(); from += BATCH_SIZE) {
                Object[] batch = group.subList(from, Math.min(from + BATCH_SIZE, group.size())).toArray();
                chain = chain.chain(() -> session.insertAll(BATCH_SIZE, batch));
            }
        return chain;
    }
}

