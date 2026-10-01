package com.guicedee.activitymaster.geography.test;

import com.guicedee.activitymaster.fsdm.client.services.*;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.fsdm.db.entities.geography.GeographyXClassification;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeoNameDefaultData;
import com.guicedee.activitymaster.geography.services.dto.GeographyCoordinates;
import com.guicedee.activitymaster.geography.services.dto.GeographyPostalCode;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import geodata.GeoDataLocation;
import geodata.GeoNamesDownloader;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.function.IntConsumer;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL assertions for ordered stateless imports across a chunk boundary. */
class GeographyBulkWriterTest {
    private static final String ENTERPRISE = "GeoStatelessTestCo";
    private static final long PARENT = 1_700_000_000L;
    private static final Duration TIMEOUT = Duration.ofMinutes(3);

    @BeforeAll static void installTaxonomy() {
        var fixture = new GeographyStatelessEnterpriseTest();
        fixture.setup();
        fixture.geographyInstall_succeeds();
    }

    @Test void americanSamoaPostalCountryLabelUsesPublishedPlaceProvince(@TempDir Path directory) throws Exception {
        Path original = GeoDataLocation.getBaseDirectory();
        GeoDataLocation.setBaseDirectory(directory);
        try {
            Files.createDirectories(GeoDataLocation.dumpDirectory());
            Files.createDirectories(GeoDataLocation.zipDirectory());
            for (String file : GeoNamesDownloader.REFERENCE_TEXT_FILES)
                Files.writeString(GeoDataLocation.dumpDirectory().resolve(file), "");
            for (String[] archive : GeoNamesDownloader.REFERENCE_ZIP_FILES)
                Files.writeString(GeoDataLocation.dumpDirectory().resolve(archive[1]), "");
            Files.writeString(GeoDataLocation.dumpDirectory().resolve("admin1CodesASCII.txt"),
                    "AS.010\tEastern District\tEastern District\t5881219\n");
            Files.writeString(GeoDataLocation.dumpDirectory().resolve("admin2Codes.txt"),
                    "AS.010.1\tMaoputasi County\tMaoputasi County\t5881136\n");
            Files.writeString(GeoDataLocation.countryGeoDataFile("AS"), """
                    5881219	Eastern District	Eastern District		-14	-170	A	ADM1	AS		010				0		0	Pacific/Pago_Pago	2019-09-05
                    5881136	Maoputasi County	Maoputasi County		-14	-170	A	ADM2	AS		010	1			0		0	Pacific/Pago_Pago	2019-09-05
                    5881576	Pago Pago	Pago Pago	Pagopago	-14.27806	-170.7025	P	PPLC	AS		010				11500	2	144	Pacific/Pago_Pago	2019-09-05
                    """);
            // Pago Pago is absent from the ADM hierarchy; the postal import must resolve its province.
            Files.writeString(GeoDataLocation.countryPostalCodeFile("AS"),
                    "AS\t96799\tPago Pago\tAs\t\t\t\t\t\t-14.2781\t-170.7025\t6\n");
            StringBuilder postalFixture = new StringBuilder(Files.readString(GeoDataLocation.countryPostalCodeFile("AS")));
            for (int i = 0; i < 130; i++) {
                int code = 97000 + i;
                postalFixture.append("AS\t").append(code).append("\tPago Pago\tAs\t\t\t\t\t\t-14.2781\t-170.7025\t6\n");
                postalFixture.append("AS\t").append(code).append("\tSuburb ").append(i).append("\tAs\t\t\t\t\t\t-14.2781\t-170.7025\t6\n");
            }
            Files.writeString(GeoDataLocation.countryPostalCodeFile("AS"), postalFixture);
            SessionUtils.<Void>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t -> {
                var session = t.getItem1(); var system = t.getItem3(); var identity = t.getItem4();
                IClassificationService<?> classifications = IGuiceContext.get(IClassificationService.class);
                IActiveFlagService<?> flags = IGuiceContext.get(IActiveFlagService.class);
                ISecurityTokenService<?> security = IGuiceContext.get(ISecurityTokenService.class);
                return classifications.find(session, "Country", NoClassificationDataConceptName, system, identity)
                        .chain(type -> flags.getActiveFlag(session, system.getEnterprise(), identity)
                                .chain(active -> {
                                    var country = new Geography();
                                    country.setName("AS"); country.setDescription("American Samoa");
                                    country.setClassificationID(type); country.setSystemID(system);
                                    country.setEnterpriseID(system.getEnterprise()); country.setActiveFlagID(active);
                                    country.setOriginalSourceSystemID(system.getId());
                                    return session.insert(country)
                                            .chain(() -> security.resolveDefaultGroupFolderTokens(session, system, identity))
                                            .chain(tokens -> country.createDefaultSecurity(session, system, system.getEnterprise(), active, tokens, identity))
                                            .chain(() -> IGuiceContext.get(IGeographyService.class).installCountry(session, system, "AS", identity))
                                            .replaceWithVoid();
                                }));
            }).await().atMost(TIMEOUT);
            Long imported = SessionUtils.<Long>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t ->
                    t.getItem1().createQuery("select count(p) from Geography p, GeographyXGeography a, GeographyXGeography b, GeographyXGeography c where p.name = '96799' and a.childGeographyID.id = p.id and b.childGeographyID.id = a.parentGeographyID.id and c.childGeographyID.id = b.parentGeographyID.id and c.parentGeographyID.name = 'AS.010'", Long.class)
                            .getSingleResult()).await().atMost(TIMEOUT);
            assertEquals(1L, imported, "Postal -> Pago Pago -> district -> published Eastern District");
            UUID postalId = SessionUtils.<UUID>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t ->
                    t.getItem1().createQuery("select g.id from Geography g where g.name = '97000' and g.classificationID.name = 'PostalCode'", UUID.class)
                            .getSingleResult()).await().atMost(TIMEOUT);
            List<Long> before = postalCounts();
            assertEquals(List.of(131L, 261L, 261L, 1827L), before, "Codes, distinct suburbs, suburb links and canonical seven grants");
            SessionUtils.<Void>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t ->
                    IGuiceContext.get(IGeographyService.class).loadCountryPostalCodes(t.getItem1(), t.getItem3(), "AS", t.getItem4()))
                    .await().atMost(TIMEOUT);
            assertEquals(before, postalCounts(), "A retry must reuse IDs and avoid duplicate rows or grants");
            UUID retainedId = SessionUtils.<UUID>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t ->
                    t.getItem1().createQuery("select g.id from Geography g where g.name = '97000' and g.classificationID.name = 'PostalCode'", UUID.class)
                            .getSingleResult()).await().atMost(TIMEOUT);
            assertEquals(postalId, retainedId);
            assertGrantMatrix(postalId);
            Map<Long, List<GeographyPostalCode>> rollback = new TreeMap<>();
            for (long i = 0; i < 128; i++) {
                var post = new GeographyPostalCode().setPostalCode(Long.toString(98000 + i))
                        .setPostalCodePlaceName("Rollback suburb " + i).setParentPlaceName("Pago Pago").setProvinceCode("AS.010");
                rollback.put(98000 + i, List.of(post));
            }
            rollback.put(9_999_999_999L, List.of(new GeographyPostalCode().setPostalCode("9999999999")
                    .setPostalCodePlaceName("Invalid code").setParentPlaceName("Pago Pago").setProvinceCode("AS.010")));
            int[] written = {0};
            assertThrows(RuntimeException.class, () -> runPostal(rollback, n -> written[0] += n).await().atMost(TIMEOUT));
            assertEquals(128, written[0], "A complete first chunk preceded the later failure");
            assertEquals(before, postalCounts(), "Later failure must roll back postal rows, relationships and grants together");
        } finally { GeoDataLocation.setBaseDirectory(original); }
    }

    private List<Long> postalCounts() {
        return SessionUtils.<List<Long>>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t ->
                t.getItem1().createQuery("select count(g) from Geography g where g.classificationID.name = 'PostalCode'", Long.class).getSingleResult()
                        .chain(codes -> t.getItem1().createQuery("select count(g) from Geography g where g.classificationID.name = 'PostalCodeSuburb'", Long.class).getSingleResult()
                                .chain(suburbs -> t.getItem1().createQuery("select count(l) from GeographyXGeography l where l.childGeographyID.classificationID.name = 'PostalCodeSuburb'", Long.class).getSingleResult()
                                        .chain(links -> t.getItem1().createQuery("select count(s) from GeographySecurityToken s where s.base.classificationID.name = 'PostalCodeSuburb'", Long.class).getSingleResult()
                                                .map(grants -> List.of(codes, suburbs, links, grants))))))
                .await().atMost(TIMEOUT);
    }

    @SuppressWarnings("unchecked")
    private Uni<Void> runPostal(Map<Long, List<GeographyPostalCode>> data, IntConsumer progress) {
        return SessionUtils.<Void>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t -> {
            try {
                Class<?> writer = Class.forName("com.guicedee.activitymaster.geography.GeographyBulkWriter");
                Method resolve = writer.getDeclaredMethod("resolve", Mutiny.StatelessSession.class, ISystems.class,
                        IClassificationService.class, IActiveFlagService.class, ISecurityTokenService.class, UUID[].class);
                Method load = writer.getDeclaredMethod("loadPostal", Mutiny.StatelessSession.class, ISystems.class,
                        Map.class, IntConsumer.class, IntConsumer.class);
                resolve.setAccessible(true); load.setAccessible(true);
                Uni<?> resolved = (Uni<?>) resolve.invoke(null, t.getItem1(), t.getItem3(),
                        IGuiceContext.get(IClassificationService.class), IGuiceContext.get(IActiveFlagService.class),
                        IGuiceContext.get(ISecurityTokenService.class), t.getItem4());
                return resolved.chain(w -> {
                    try { return (Uni<Void>) load.invoke(w, t.getItem1(), t.getItem3(), data, (IntConsumer) n -> {}, progress); }
                    catch (ReflectiveOperationException e) { return Uni.createFrom().failure(e); }
                });
            } catch (ReflectiveOperationException e) { return Uni.createFrom().failure(e); }
        });
    }

    @Test void batchesPreserveIdsGrantsAndRetryLinksAndRollBackTogether() {
        UUID parentId = SessionUtils.<UUID>withSystemAndTokenStateless(ENTERPRISE,
                IGeographyService.GeographySystemName, t -> {
            var session = t.getItem1(); var system = t.getItem3(); var identity = t.getItem4();
            IClassificationService<?> cs = IGuiceContext.get(IClassificationService.class);
            IActiveFlagService<?> flags = IGuiceContext.get(IActiveFlagService.class);
            return cs.find(session, "City", NoClassificationDataConceptName, system, identity)
                    .chain(city -> cs.find(session, "GeoNameID", NoClassificationDataConceptName, system, identity)
                            .chain(source -> flags.getActiveFlag(session, system.getEnterprise(), identity)
                                    .chain(active -> {
                                        var parent = new Geography();
                                        parent.setName("Bulk test district"); parent.setDescription("Bulk test district");
                                        parent.setEnterpriseID(system.getEnterprise()); parent.setSystemID(system);
                                        parent.setActiveFlagID(active); parent.setClassificationID(city);
                                        parent.setOriginalSourceSystemID(system.getId());
                                        var link = new GeographyXClassification();
                                        link.setGeographyID(parent); link.setClassificationID(source);
                                        link.setValue(Long.toString(PARENT)); link.setSystemID(system);
                                        link.setEnterpriseID(system.getEnterprise()); link.setActiveFlagID(active);
                                        link.setOriginalSourceSystemID(system.getId());
                                        return session.insert(parent).chain(() -> session.insert(link)).replaceWith(parent.getId());
                                    })));
        }).await().atMost(TIMEOUT);

        Map<Long, GeoNameDefaultData<?>> data = fixture(130, PARENT + 1);
        Map<Long, List<Long>> hierarchy = Map.of(PARENT, data.keySet().stream().filter(id -> id != PARENT).toList());
        int[] total = {0}, progress = {0};
        run(data, hierarchy, n -> total[0] = n, n -> progress[0] += n).await().atMost(TIMEOUT);
        assertEquals(130, total[0]); assertEquals(130, progress[0]);
        Set<UUID> ids = new HashSet<>();
        data.forEach((source, dto) -> { if (source != PARENT) assertTrue(ids.add(dto.getGeographyId()), "Distinct source IDs with the same name must remain distinct"); });
        assertEquals(130, ids.size()); assertFalse(ids.contains(null));

        assertCounts(ids, 130, 780, 130, 910, 5460, 910);
        assertGrantMatrix(ids.iterator().next());
        Map<Long, UUID> originalIds = new HashMap<>();
        data.forEach((source, dto) -> originalIds.put(source, dto.getGeographyId()));
        run(data, hierarchy, n -> {}, n -> {}).await().atMost(TIMEOUT);
        assertCounts(ids, 130, 780, 130, 910, 5460, 910);
        data.forEach((source, dto) -> assertEquals(originalIds.get(source), dto.getGeographyId()));

        // Simulate an import that previously persisted the child but omitted its parent link.
        UUID missingChild = ids.iterator().next();
        SessionUtils.<Void>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t ->
                t.getItem1().createQuery("delete from GeographyXGeographySecurityToken s where s.base.id in (select l.id from GeographyXGeography l where l.parentGeographyID.id = :parent and l.childGeographyID.id = :child)")
                        .setParameter("parent", parentId).setParameter("child", missingChild).executeUpdate()
                        .chain(() -> t.getItem1().createQuery("delete from GeographyXGeography l where l.parentGeographyID.id = :parent and l.childGeographyID.id = :child")
                                .setParameter("parent", parentId).setParameter("child", missingChild).executeUpdate()).replaceWithVoid())
                .await().atMost(TIMEOUT);
        run(data, hierarchy, n -> {}, n -> {}).await().atMost(TIMEOUT);
        assertCounts(ids, 130, 780, 130, 910, 5460, 910);

        // A later chunk failing must also undo the first chunk on the caller's transaction.
        var failed = fixture(130, PARENT + 1000);
        failed.get(PARENT + 1129).setName("x".repeat(5000));
        var failedHierarchy = Map.of(PARENT, failed.keySet().stream().filter(id -> id != PARENT).toList());
        assertThrows(Exception.class, () -> run(failed, failedHierarchy, n -> {}, n -> {}).await().atMost(TIMEOUT));
        Set<UUID> failedIds = new HashSet<>();
        failed.forEach((source, dto) -> { if (dto.getGeographyId() != null) failedIds.add(dto.getGeographyId()); });
        assertTrue(failedIds.size() >= 128, "The first batch was attempted before failure");
        assertCounts(failedIds, 0, 0, 0, 0, 0, 0);
    }

    private static Map<Long, GeoNameDefaultData<?>> fixture(int count, long first) {
        Map<Long, GeoNameDefaultData<?>> data = new LinkedHashMap<>();
        var parent = new GeoNameDefaultData<>(); parent.setGeonameId(PARENT); data.put(PARENT, parent);
        for (int i = 0; i < count; i++) {
            var place = new GeoNameDefaultData<>();
            place.setGeonameId(first + i); place.setName("Same place name"); place.setAsciiname("Same place name");
            place.setCoordinates(new GeographyCoordinates("1.25", "2.5"));
            place.setPopulation(10); place.setElevation(20); place.setDem(30); data.put(first + i, place);
        }
        return data;
    }

    private static Uni<Void> run(Map<Long, GeoNameDefaultData<?>> data, Map<Long, List<Long>> hierarchy,
            IntConsumer total, IntConsumer progress) {
        return SessionUtils.withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t -> {
            try {
                Class<?> writer = Class.forName("com.guicedee.activitymaster.geography.GeographyBulkWriter");
                Method resolve = writer.getDeclaredMethod("resolve", Mutiny.StatelessSession.class, ISystems.class,
                        IClassificationService.class, IActiveFlagService.class, ISecurityTokenService.class, UUID[].class);
                Method load = writer.getDeclaredMethod("load", Mutiny.StatelessSession.class, ISystems.class,
                        Map.class, Map.class, IntConsumer.class, IntConsumer.class);
                resolve.setAccessible(true); load.setAccessible(true);
                Uni<?> resolved = (Uni<?>) resolve.invoke(null, t.getItem1(), t.getItem3(),
                        IGuiceContext.get(IClassificationService.class), IGuiceContext.get(IActiveFlagService.class),
                        IGuiceContext.get(ISecurityTokenService.class), t.getItem4());
                return resolved.chain(w -> {
                    try { return (Uni<Void>) load.invoke(w, t.getItem1(), t.getItem3(), data, hierarchy, total, progress); }
                    catch (ReflectiveOperationException e) { return Uni.createFrom().failure(e); }
                });
            } catch (ReflectiveOperationException e) { return Uni.createFrom().failure(e); }
        });
    }

    private static void assertCounts(Set<UUID> ids, long... expected) {
        List<String> hql = List.of("from Geography g where g.id in :ids",
                "from GeographyXClassification g where g.geographyID.id in :ids",
                "from GeographyXGeography g where g.childGeographyID.id in :ids",
                "from GeographySecurityToken g where g.base.id in :ids",
                "from GeographyXClassificationSecurityToken g where g.base.geographyID.id in :ids",
                "from GeographyXGeographySecurityToken g where g.base.childGeographyID.id in :ids");
        List<Long> counts = SessionUtils.<List<Long>>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t -> {
            List<Long> results = new ArrayList<>(); Uni<Void> chain = Uni.createFrom().voidItem();
            for (String query : hql) chain = chain.chain(() -> t.getItem1().createQuery("select count(g) " + query, Long.class)
                    .setParameter("ids", ids).getSingleResult().invoke(results::add).replaceWithVoid());
            return chain.replaceWith(results);
        }).await().atMost(TIMEOUT);
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], counts.get(i), hql.get(i));
    }

    private static void assertGrantMatrix(UUID id) {
        var grants = SessionUtils.<List<Object[]>>withSystemAndTokenStateless(ENTERPRISE, IGeographyService.GeographySystemName, t ->
                t.getItem1().createQuery("select s.securityTokenID.name, s.createAllowed, s.updateAllowed, s.deleteAllowed, s.readAllowed from GeographySecurityToken s where s.base.id = :id", Object[].class)
                        .setParameter("id", id).getResultList()).await().atMost(TIMEOUT);
        assertEquals(7, grants.size());
        Map<String, List<Boolean>> expected = Map.of("administrators", List.of(true,true,true,true),
                "everyone", List.of(false,false,false,false), "everywhere", List.of(false,false,false,true),
                "system", List.of(true,true,false,true), "applications", List.of(true,true,false,true),
                "plugins", List.of(true,true,false,true), "guests", List.of(false,false,false,true));
        for (Object[] grant : grants) assertEquals(expected.get(grant[0].toString().toLowerCase(Locale.ROOT)),
                List.of(grant[1], grant[2], grant[3], grant[4]), "Grant for " + grant[0]);
    }
}
