package com.guicedee.activitymaster.geography;

/**
 * Reactivity Migration Checklist:
 * <p>
 * [✓] One action per Mutiny.StatelessSession at a time
 * [✓] Pass Mutiny.StatelessSession through the chain
 * [✓] No await() usage
 * [✓] No parallel operations on a session
 * [✓] No session/transaction creation in libraries
 */

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.guicedee.activitymaster.fsdm.client.services.IActiveFlagService;
import com.guicedee.activitymaster.fsdm.client.services.IActivityMasterService;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.systems.IProgressable;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography_;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.*;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.client.IGuiceContext;
import geodata.GeoDataFinder;
import geodata.GeoDataLocation;
import geodata.GeoNamesDownloader;
import geodata.PostalProvinceResolver;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.persistence.NoResultException;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.reactive.mutiny.Mutiny;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.NoClassification;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.modules.services.jsonrepresentation.json.StaticStrings.STRING_EMPTY;
import static geodata.GeoDataFiles.*;

@Log4j2
@Singleton
public class GeographyService implements IProgressable, IGeographyService<GeographyService> {
    @Inject
    private PlanetService planetService;

    @Inject
    private ContinentService continentService;

    @Inject
    private CountryService countryService;

    @Inject
    private ProvinceService provinceService;

    @Inject
    private DistrictService districtService;

    @Inject
    private TownService townService;

    @Inject
    private PostalCodeService postalCodeService;

    @Inject
    private CurrencyService currencyService;

    @Inject
    private TimeZoneService timeZoneService;

    @Inject
    private LanguagesService languagesService;

    @Inject
    private IClassificationService<?> classificationService;

    @Inject
    private GeographySecurityCollector securityCollector;

    public Uni<IClassification<?, ?>> findFeatureClass(Mutiny.StatelessSession session,
                                                       GeographyFeatureClassesClassifications key,
                                                       ISystems<?, ?> system,
                                                       UUID... identityToken) {
        return classificationService.find(session, key.toString(), system, identityToken);
    }

    public Uni<Geography> findGeographyByID(Mutiny.StatelessSession session, UUID geographyID) {
        if (geographyID == null) return Uni.createFrom().nullItem();
        return session.get(Geography.class, geographyID);
    }

    // ---------------------------------------------------------------------------------------------
    //  GeoNames download + per-country installation
    // ---------------------------------------------------------------------------------------------

    @Override
    public Uni<Void> downloadCountryData(String countryCode, UUID... identityToken) {
        String cc = countryCode.toUpperCase();
        return Uni.createFrom().<Void>item(() -> {
            try {
                GeoNamesDownloader downloader = new GeoNamesDownloader();
                logProgress("Geography Download", "Downloading GeoNames reference data");
                downloader.downloadReferenceData();
                logProgress("Geography Download", "Downloading GeoNames data for " + cc);
                downloader.downloadCountry(cc);
                log.info("GeoNames data for {} downloaded into {}",
                         cc,
                         GeoDataLocation.getBaseDirectory().toAbsolutePath());
                return null;
            } catch (IOException e) {
                log.error("Failed to download GeoNames data for {}: {}", cc, e.getMessage(), e);
                throw new RuntimeException("Failed to download GeoNames data for " + cc, e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    /**
     * Ensures the per-country GeoNames files exist on disk, downloading them on a worker thread when absent.
     */
    private Uni<Void> ensureCountryFilesDownloaded(String cc) {
        Path geoData = GeoDataLocation.countryGeoDataFile(cc);
        Path postalCodes = GeoDataLocation.countryPostalCodeFile(cc);
        if (Files.exists(geoData) && Files.exists(postalCodes)) {
            log.info("GeoNames files for {} already present in {}",
                     cc,
                     GeoDataLocation.getBaseDirectory().toAbsolutePath());
            return Uni.createFrom().voidItem();
        }
        return downloadCountryData(cc);
    }

    /**
     * Loads the hierarchy entries (ADM only) into the supplied map, preferring the downloaded
     * {@code hierarchy.txt} and falling back to the bundled {@code hierarchy.csv} resource.
     */
    private void loadHierarchyInto(Map<Long, List<Long>> hierarchyMap) {
        Path downloadedHierarchy = GeoDataLocation.dumpDirectory().resolve("hierarchy.txt");
        if (Files.exists(downloadedHierarchy)) {
            try (GeoDataFinder finder = new GeoDataFinder(downloadedHierarchy,
                                                          CSVFormat.TDF,
                                                          Hierarchy.getHeaderNames())) {
                collectHierarchy(finder, hierarchyMap);
                return;
            } catch (Exception e) {
                log.warn("Failed to read downloaded hierarchy file, falling back to bundled resource: {}",
                         e.getMessage());
            }
        }
        try (GeoDataFinder finder = new GeoDataFinder(Hierarchy, CSVFormat.TDF, Hierarchy.getHeaderNames())) {
            collectHierarchy(finder, hierarchyMap);
        } catch (Exception e) {
            log.error("Error loading hierarchy data", e);
            throw new RuntimeException("Error loading hierarchy data", e);
        }
    }

    private void collectHierarchy(GeoDataFinder finder, Map<Long, List<Long>> hierarchyMap) {
        for (CSVRecord record : finder.getRecords()) {
            if (!"ADM".equals(record.get("type"))) continue;
            Long parentId = Long.valueOf(record.get(0));
            Long childId = Long.valueOf(record.get(1));
            hierarchyMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
        }
    }

    // =============================================================================================
    //  Stateless twins
    // =============================================================================================

    private <T> Uni<T> withGeographySystemContext(Mutiny.StatelessSession session,
                                                  ISystems<?, ?> requestingSystem,
                                                  java.util.function.BiFunction<ISystems<?, ?>, UUID[], Uni<T>> work) {
        var enterprise = requestingSystem.getEnterprise();
        return IActivityMasterService.getISystem(session, GeographySystemName, enterprise)
                                     .chain(geoSystem -> IActivityMasterService
                                             .getISystemToken(session, GeographySystemName, enterprise)
                                             .chain(geoToken -> work.apply(geoSystem, new UUID[]{geoToken})));
    }

    @Override
    public Uni<IGeography<?, ?>> createPlanet(Mutiny.StatelessSession session,
                                              @NotNull String value,
                                              String originalUniqueID,
                                              ISystems<?, ?> system,
                                              UUID... identityToken) {
        return planetService.createPlanet(session,
                                          value,
                                          "The planet " + value,
                                          originalUniqueID,
                                          system,
                                          identityToken);
    }

    @Override
    public Uni<IGeography<?, ?>> createContinent(Mutiny.StatelessSession session,
                                                 String planetName,
                                                 GeographyContinent continent,
                                                 ISystems<?, ?> originatingSystem,
                                                 String originalUniqueID,
                                                 UUID... identityToken) {
        return planetService.findPlanet(session, planetName, originatingSystem, identityToken)
                            .chain(planet -> continentService.createContinent(session,
                                                                              planet,
                                                                              continent.getContinentCode(),
                                                                              continent.getContinentName(),
                                                                              originalUniqueID,
                                                                              originatingSystem,
                                                                              identityToken));
    }

    @Override
    public Uni<IGeography<?, ?>> findPlanet(Mutiny.StatelessSession session,
                                            String name,
                                            ISystems<?, ?> originatingSystem,
                                            UUID... identityToken) {
        return planetService.findPlanet(session, name, originatingSystem, identityToken);
    }

    @Override
    public Uni<GeographyContinent> findContinent(Mutiny.StatelessSession session,
                                                 GeographyContinent continent,
                                                 ISystems<?, ?> system,
                                                 UUID... identityToken) {
        return continentService.findContinent(session, continent.getContinentCode(), system, identityToken)
                               .map(continentGeo -> {
                                   GeographyContinent gc = new GeographyContinent();
                                   gc.setContinentCode(continentGeo.getName());
                                   gc.setContinentName(continentGeo.getDescription());
                                   gc.setGeographyId(continentGeo.getId());
                                   return gc;
                               });
    }

    @Override
    public Uni<GeographyCountry> findCountry(Mutiny.StatelessSession session,
                                             GeographyCountry country,
                                             ISystems<?, ?> system,
                                             UUID... identityToken) {
        return countryService.findCountry(session, country.getIso(), system, identityToken).chain(geo -> {
            GeographyCountry gc = new GeographyCountry();
            gc.setGeographyId(geo.getId());
            gc.setIso(country.getIso());
            return geo.findClassification(session, GeoNameID, system, identityToken)
                      .onFailure(NoResultException.class).recoverWithNull().onItem().ifNotNull()
                      .invoke(rel -> gc.setGeonameId(Long.parseLong(rel.getValue())))
                      .chain(() -> describeCountryResources(session, geo, gc, system, identityToken)).replaceWith(gc);
        });
    }

    public Uni<GeographyCountry> createCountry(Mutiny.StatelessSession session,
                                               GeographyCountry country,
                                               ISystems<?, ?> system,
                                               UUID... identityToken) {
        return continentService.findContinent(session, country.getContinent().getContinentCode(), system, identityToken)
                               .chain(geoContinent -> countryService.createCountry(session,
                                                                                   geoContinent,
                                                                                   country.getIso(),
                                                                                   country.getCountryName(),
                                                                                   country.getGeonameId() + "",
                                                                                   system,
                                                                                   identityToken))
                               .chain(geoCountry -> currencyService.createCurrency(session,
                                                                                   country.getCurrency()
                                                                                          .getCurrencyCode(),
                                                                                   country.getCurrency()
                                                                                          .getCurrencyName(),
                                                                                   system,
                                                                                   identityToken)
                                                                   .chain(currency -> geoCountry
                                                                           .addOrUpdateClassification(session,
                                                                                                      Currency,
                                                                                                      currency.getName(),
                                                                                                      currency.getName(),
                                                                                                      system,
                                                                                                      identityToken)
                                                                           .chain(() -> countryService.updateCountry(
                                                                                   session,
                                                                                   currency,
                                                                                   country.getIso(),
                                                                                   country.getCountryName(),
                                                                                   country.getIso3(),
                                                                                   country.getIsoNumeric(),
                                                                                   country.getCountryDialCode(),
                                                                                   country.getFips(),
                                                                                   country.getCapital(),
                                                                                   country.getAreaSqlKM(),
                                                                                   country.getPostalCodeDecimalFormat(),
                                                                                   country.getPostalCodeRegexFormat(),
                                                                                   country.getPopulation(),
                                                                                   country.getWebTld(),
                                                                                   system,
                                                                                   identityToken))).map(updated -> {
                                           country.setGeographyId(geoCountry.getId());
                                           return country;
                                       }));
    }

    private static final String ACTIVE_GEOGRAPHY = " join dbo.activeflag f on f.activeflagid = g.activeflagid"
            + " where g.enterpriseid = :enterprise and f.allowaccess = 1"
            + " and g.effectivefromdate <= statement_timestamp() and g.effectivetodate > statement_timestamp()";

    @Override
    public Uni<List<GeographyCountry>> listCountries(Mutiny.StatelessSession session,
                                                     ISystems<?, ?> system,
                                                     UUID... identityToken) {
        Objects.requireNonNull(system, "system");
        return session.createNativeQuery("select g.geographyname, g.geographydesc, g.geographyid from geography.geography g"
                                                 + " join classification.classification c on c.classificationid = g.classificationid"
                                                 + ACTIVE_GEOGRAPHY
                                                 + " and c.classificationname = :country and c.enterpriseid = :enterprise"
                                                 + " order by g.geographydesc, g.geographyname", Object[].class)
                      .setParameter("enterprise", system.getEnterprise().getId())
                      .setParameter("country", Country.toString())
                      .getResultList()
                      .chain(rows -> {
                          Map<String, GeographyCountry> countries = new LinkedHashMap<>();
                          for (Object[] row : rows) {
                              String iso = (String) row[0];
                              if (iso == null || countries.containsKey(iso)) continue;
                              GeographyCountry country = new GeographyCountry();
                              country.setIso(iso);
                              country.setGeographyId((UUID) row[2]);
                              country.setCountryName(row[1] == null || ((String) row[1]).isBlank() ? iso : (String) row[1]);
                              countries.put(iso, country);
                          }
                          Uni<Void> descriptions = Uni.createFrom().voidItem();
                          for (var country : countries.values()) {
                              Geography row = new Geography().setId(country.getGeographyId());
                              row.setName(country.getIso());
                              row.setEnterpriseID(system.getEnterprise());
                              descriptions = descriptions.chain(() -> describeCountryResources(session, row, country, system, identityToken));
                          }
                          return descriptions.replaceWith(() -> List.copyOf(countries.values()));
                      });
    }

    private Uni<Void> describeCountryResources(Mutiny.StatelessSession session, IGeography<?, ?> country,
                                               GeographyCountry result, ISystems<?, ?> system, UUID... identityToken) {
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (var provider : ServiceLoader.load(com.guicedee.activitymaster.geography.services.IGeographyCountryResourceProvider.class)) {
            chain = chain.chain(() -> provider.describeResources(session, country, result, system, identityToken));
        }
        return chain;
    }

    @Override
    public Uni<List<String>> searchPlaces(Mutiny.StatelessSession session,
                                          String countryIso,
                                          String prefix,
                                          int limit,
                                          ISystems<?, ?> system,
                                          UUID... identityToken) {
        Objects.requireNonNull(system, "system");
        if (countryIso == null || !countryIso.matches("[A-Za-z]{2}"))
            return Uni.createFrom().failure(new IllegalArgumentException("A two-letter country code is required"));
        if (prefix == null || prefix.isBlank() || prefix.length() > 128)
            return Uni.createFrom().failure(new IllegalArgumentException("A search prefix of 1 to 128 characters is required"));
        if (limit < 1 || limit > 100)
            return Uni.createFrom().failure(new IllegalArgumentException("The limit must be between 1 and 100"));
        String pattern = prefix.strip().toLowerCase(Locale.ROOT)
                               .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        // Country -> province -> district -> town, as built by the per-country geodata install.
        String sql = "with recursive tree(id, depth) as ("
                + " select g.geographyid, 0 from geography.geography g"
                + " join classification.classification c on c.classificationid = g.classificationid"
                + ACTIVE_GEOGRAPHY
                + " and g.geographyname = :iso and c.classificationname = :country and c.enterpriseid = :enterprise"
                + " union all"
                + " select x.childgeographyid, t.depth + 1 from geography.geographyxgeography x"
                + " join tree t on x.parentgeographyid = t.id"
                + " where t.depth < 3 and x.enterpriseid = :enterprise"
                + " and x.effectivefromdate <= statement_timestamp() and x.effectivetodate > statement_timestamp())"
                + " select distinct g.geographyname from tree t"
                + " join geography.geography g on g.geographyid = t.id"
                + " join classification.classification c on c.classificationid = g.classificationid"
                + ACTIVE_GEOGRAPHY
                + " and t.depth > 0 and c.classificationname in (:town, :city) and c.enterpriseid = :enterprise"
                + " and lower(g.geographyname) like :pattern escape '\\'"
                + " order by g.geographyname limit :limit";
        return session.createNativeQuery(sql, String.class)
                      .setParameter("enterprise", system.getEnterprise().getId())
                      .setParameter("iso", countryIso.toUpperCase(Locale.ROOT))
                      .setParameter("country", Country.toString())
                      .setParameter("town", Town.toString())
                      .setParameter("city", City.toString())
                      .setParameter("pattern", pattern)
                      .setParameter("limit", limit)
                      .getResultList()
                      .map(List::copyOf);
    }

    @Override
    public Uni<GeographyCountry> findCountryDetailed(Mutiny.StatelessSession session,
                                                     String iso,
                                                     ISystems<?, ?> system,
                                                     UUID... identityToken) {
        return countryService.findCountry(session, iso, system, identityToken).chain(geo -> {
            GeographyCountry gc = new GeographyCountry();
            gc.setGeographyId(geo.getId());
            gc.setIso(geo.getName());
            gc.setCountryName(geo.getDescription());
            return geo.findClassificationValues(session, system, identityToken).invoke(values -> {
                String geoName = values.get(GeoNameID.toString());
                if (geoName != null && !geoName.isBlank()) {
                    try {
                        gc.setGeonameId(Long.parseLong(geoName.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                gc.setIso3(values.get(CountryISO3166_3.toString()));
                gc.setIsoNumeric(values.get(CountryISO_Numeric.toString()));
                gc.setFips(values.get(CountryFips.toString()));
                gc.setCapital(values.get(CountryCapital.toString()));
                gc.setAreaSqlKM(values.get(CountryAreaInSqKm.toString()));
                gc.setWebTld(values.get(CountryTld.toString()));
                gc.setCountryDialCode(values.get(CountryPhone.toString()));
                gc.setPostalCodeDecimalFormat(values.get(CountryPostalCodeFormat.toString()));
                gc.setPostalCodeRegexFormat(values.get(CountryPostalCodeRegex.toString()));
                String population = values.get(Population.toString());
                if (population != null && !population.isBlank()) {
                    try {
                        gc.setPopulation(Integer.parseInt(population.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }).chain(() -> describeCountryResources(session, geo, gc, system, identityToken)).replaceWith(gc);
        });
    }

    @Override
    public Uni<Void> loadProvincesASCII1(Mutiny.StatelessSession session,
                                         ISystems<?, ?> requestingSystem,
                                         String countryCode,
                                         UUID... requestingToken) {
        return withGeographySystemContext(session, requestingSystem, (system, identityToken) -> {
            setCurrentTask(0);
            securityCollector.activate(session);
            return Uni.createFrom().item(() -> {
                          Path reference = GeoDataLocation.dumpDirectory().resolve("admin1CodesASCII.txt");
                          try (GeoDataFinder finder = Files.exists(reference)
                                  ? new GeoDataFinder(reference, CSVFormat.TDF, Admin1CodesASCII.getHeaderNames())
                                  : new GeoDataFinder(Admin1CodesASCII,
                                                                        CSVFormat.TDF,
                                                                        Admin1CodesASCII.getHeaderNames())) {
                              List<GeographyAsciiCode> records = new ArrayList<>();
                              for (CSVRecord record : finder.getRecords()) {
                                  GeographyAsciiCode ascii = new GeographyAsciiCode();
                                  ascii.setCode(record.get(0)).setName(record.get(1)).setNameAscii(record.get(2))
                                       .setGeonameId(Long.parseLong(record.get(3)));
                                  if (ascii.getCode().startsWith(countryCode + ".")) records.add(ascii);
                              }
                              setTotalTasks(records.size());
                              return records;
                          } catch (Exception e) {
                              log.error("Error loading province codes", e);
                              throw new RuntimeException("Error loading province codes", e);
                          }
                      }).chain(records -> {
                          Uni<Void> chain = Uni.createFrom().voidItem();
                          for (GeographyAsciiCode ascii : records)
                              chain = chain.chain(() -> countryService.findCountry(session, countryCode, system)
                                                                      .chain(country -> provinceService.createProvince(session,
                                                                                                                       country,
                                                                                                                       ascii.getCode(),
                                                                                                                       ascii.getName(),
                                                                                                                       ascii.getGeonameId() + "",
                                                                                                                       system))
                                                                      .invoke(p -> logProgress("Geography Service",
                                                                                               "Loaded Province Codes - " + ascii.getName(),
                                                                                               1)).replaceWithVoid());
                          return chain;
                      }).chain(() -> securityCollector.flush(session, system, identityToken))
                      .invoke(() -> logProgress("Geography Service", "Finished Province Codes")).onFailure()
                      .invoke(e -> log.error("Error loading province codes: {}", e.getMessage(), e));
        });
    }

    @Override
    public Uni<Void> loadDistrictsASCII2(Mutiny.StatelessSession session,
                                         ISystems<?, ?> requestingSystem,
                                         String countryCode,
                                         UUID... requestingToken) {
        return withGeographySystemContext(session, requestingSystem, (system, identityToken) -> {
            setCurrentTask(0);
            securityCollector.activate(session);
            return Uni.createFrom().item(() -> {
                          Path reference = GeoDataLocation.dumpDirectory().resolve("admin2Codes.txt");
                          try (GeoDataFinder finder = Files.exists(reference)
                                  ? new GeoDataFinder(reference, CSVFormat.TDF, Admin2Codes.getHeaderNames())
                                  : new GeoDataFinder(Admin2Codes,
                                                                        CSVFormat.TDF,
                                                                        Admin2Codes.getHeaderNames())) {
                              List<GeographyAsciiCode> records = new ArrayList<>();
                              for (CSVRecord record : finder.getRecords()) {
                                  GeographyAsciiCode ascii = new GeographyAsciiCode();
                                  ascii.setCode(record.get(0)).setName(record.get(1)).setNameAscii(record.get(2))
                                       .setGeonameId(Long.parseLong(record.get(3)));
                                  int loc = ascii.getCode().indexOf('.', 4);
                                  String pc = ascii.getCode().substring(0, loc);
                                  if (pc.startsWith(countryCode)) records.add(ascii);
                              }
                              setTotalTasks(records.size());
                              return records;
                          } catch (Exception e) {
                              log.error("Error loading district codes", e);
                              throw new RuntimeException("Error loading district codes", e);
                          }
                      }).chain(records -> {
                          Uni<Void> chain = Uni.createFrom().voidItem();
                          for (GeographyAsciiCode ascii : records) {
                              int loc = ascii.getCode().indexOf('.', 4);
                              String pc = ascii.getCode().substring(0, loc);
                              chain = chain.chain(() -> provinceService.findProvince(session, pc, system)
                                                                       .chain(province -> districtService.createDistrict(session,
                                                                                                                         province,
                                                                                                                         ascii.getCode(),
                                                                                                                         ascii.getName(),
                                                                                                                         ascii.getGeonameId() + "",
                                                                                                                         system))
                                                                       .invoke(d -> logProgress("Geography Service",
                                                                                                "Loaded District - " + ascii.getName(),
                                                                                                1)).replaceWithVoid());
                          }
                          return chain;
                      }).chain(() -> securityCollector.flush(session, system, identityToken))
                      .invoke(() -> logProgress("Geography Service", "Finished Districts/Cities")).onFailure()
                      .invoke(e -> log.error("Error loading district codes: {}", e.getMessage(), e));
        });
    }

    @Override
    public Uni<Void> loadLanguages(Mutiny.StatelessSession session,
                                   ISystems<?, ?> requestingSystem,
                                   UUID... requestingToken) {
        return withGeographySystemContext(session, requestingSystem, (system, identityToken) -> {
            setCurrentTask(0);
            return Uni.createFrom().item(() -> {
                try (GeoDataFinder finder = new GeoDataFinder(ISO639Languages,
                                                              CSVFormat.TDF,
                                                              ISO639Languages.getHeaderNames())) {
                    List<ISO639Language> languages = new ArrayList<>();
                    for (CSVRecord record : finder.getRecords()) {
                        ISO639Language language = new ISO639Language();
                        String code2 = record.get(0), code1 = record.get(1), english = record.get(2), french = record.get(
                                3), german = record.get(4);
                        language.setIso6391Code(code1);
                        language.setIso6392Code(code2);
                        if (!english.isEmpty()) {
                            StringTokenizer st = new StringTokenizer(english, ";");
                            while (st.hasMoreTokens()) language.getName().add(st.nextToken());
                        }
                        if (!french.isEmpty()) {
                            StringTokenizer st = new StringTokenizer(french, ";");
                            while (st.hasMoreTokens()) language.getFrenchName().add(st.nextToken());
                        }
                        if (!german.isEmpty()) {
                            StringTokenizer st = new StringTokenizer(german, ";");
                            while (st.hasMoreTokens()) language.getGermanName().add(st.nextToken());
                        }
                        languages.add(language);
                    }
                    setTotalTasks(languages.size());
                    return languages;
                } catch (Exception e) {
                    log.error("Error loading languages", e);
                    throw new RuntimeException("Error loading languages", e);
                }
            }).chain(languages -> {
                Uni<Void> chain = Uni.createFrom().voidItem();
                for (ISO639Language language : languages)
                    chain = chain.chain(() -> {
                        String english = language.getName().isEmpty() ? "" : language.getName().iterator().next();
                        return languagesService.createLanguage(session,
                                                               language.getIso6391Code(),
                                                               english,
                                                               language.getIso6391Code(),
                                                               system).chain(lang -> languagesService.updateLanguage(
                                session,
                                lang.getName(),
                                null,
                                language.getIso6392Code(),
                                null,
                                null,
                                null,
                                system)).chain(updated -> {
                            Uni<?> nc = Uni.createFrom().voidItem();
                            for (String s : language.getName())
                                nc = nc.chain(() -> languagesService
                                        .updateLanguage(session, updated.getName(), null, null, s, null, null, system)
                                        .replaceWithVoid());
                            for (String s : language.getFrenchName())
                                nc = nc.chain(() -> languagesService
                                        .updateLanguage(session, updated.getName(), null, null, null, s, null, system)
                                        .replaceWithVoid());
                            for (String s : language.getGermanName())
                                nc = nc.chain(() -> languagesService
                                        .updateLanguage(session, updated.getName(), null, null, null, null, s, system)
                                        .replaceWithVoid());
                            return nc.replaceWithVoid();
                        }).invoke(() -> logProgress("Geography Service",
                                                    "Loading Language - " + language.getIso6391Code(),
                                                    1));
                    });
                return chain;
            }).onFailure().invoke(e -> log.error("Error loading languages: {}", e.getMessage(), e));
        });
    }

    @Override
    public Uni<Void> loadCountryInfo(Mutiny.StatelessSession session,
                                     ISystems<?, ?> requestingSystem,
                                     UUID... requestingToken) {
        return withGeographySystemContext(session, requestingSystem, (system, identityToken) -> {
            setCurrentTask(0);
            securityCollector.activate(session);
            return Uni.createFrom().item(() -> {
                          try (GeoDataFinder finder = new GeoDataFinder(CountryInfo,
                                                                        CSVFormat.TDF,
                                                                        CountryInfo.getHeaderNames())) {
                              List<CSVRecord> records = new ArrayList<>();
                              for (CSVRecord record : finder.getRecords()) records.add(record);
                              setTotalTasks(records.size());
                              return records;
                          } catch (Exception e) {
                              log.error("Error loading country info", e);
                              throw new RuntimeException("Error loading country info", e);
                          }
                      }).chain(records -> {
                          Uni<Void> chain = Uni.createFrom().voidItem();
                          for (CSVRecord record : records)
                              chain = chain.chain(() -> {
                                  GeographyCountry country = new GeographyCountry();
                                  country.setIso(record.get(0));
                                  country.setIso3(record.get(1));
                                  country.setIsoNumeric(record.get(2));
                                  country.setFips(record.get(3));
                                  country.setCountryName(record.get(4));
                                  country.setCapital(record.get(5));
                                  country.setAreaSqlKM(record.get(6));
                                  try {
                                      country.setPopulation(Integer.parseInt(record.get(7)));
                                  } catch (NumberFormatException nfe) {
                                      country.setPopulation(0);
                                  }
                                  String continentCode = record.get(8);
                                  return findContinent(session,
                                                       new GeographyContinent().setContinentCode(continentCode),
                                                       system,
                                                       identityToken).chain(gc -> {
                                      country.setContinent(gc);
                                      country.setWebTld(record.get(9));
                                      return currencyService.createCurrency(session,
                                                                            record.get(10),
                                                                            record.get(11),
                                                                            system,
                                                                            identityToken);
                                  }).chain(currencyClassification -> {
                                      GeographyCurrency gcc = new GeographyCurrency()
                                              .setCurrencyCode(currencyClassification.getName())
                                              .setCurrencyName(currencyClassification.getDescription());
                                      country.setCurrency(gcc);
                                      country.setCountryDialCode(record.get(12));
                                      country.setPostalCodeDecimalFormat(record.get(13));
                                      country.setPostalCodeRegexFormat(record.get(14));
                                      try {
                                          country.setGeonameId(Long.parseLong(record.get(16)));
                                      } catch (NumberFormatException nfe) {
                                      }
                                      if (record.size() > 18) country.setEquivalentFips(record.get(18));
                                      return createCountry(session, country, system, identityToken);
                                  }).invoke(g -> logProgress("Geography Service",
                                                             "Loaded Country " + country.getCountryName(),
                                                             1))
                                    .onFailure().transform(e -> new GeographyException(
                                            "Unable to load country " + record.get(0) + ": " + e.getMessage(), e))
                                    .replaceWithVoid();
                              });
                          return chain;
                      }).chain(() -> securityCollector.flush(session, system, identityToken))
                      .invoke(() -> logProgress("Geography Service", "Finished Loading Countries")).onFailure()
                      .invoke(e -> log.error("Error loading country info: {}", e.getMessage(), e));
        });
    }

    @Override
    public Uni<GeographyTimezone> findTimezone(Mutiny.StatelessSession session,
                                               GeographyTimezone timezone,
                                               ISystems<?, ?> system,
                                               UUID... identityToken) {
        return timeZoneService.findTimeZone(session, timezone.getTimezoneID(), system, identityToken).map(tzc -> {
            GeographyTimezone tz = new GeographyTimezone();
            tz.setTimezoneID(tzc.getName());
            return tz;
        });
    }

    @Override
    public Uni<Void> loadTimeZones(Mutiny.StatelessSession session,
                                   ISystems<?, ?> requestingSystem,
                                   UUID... requestingToken) {
        return withGeographySystemContext(session, requestingSystem, (system, identityToken) -> {
            setCurrentTask(0);
            return Uni.createFrom().item(() -> {
                try (GeoDataFinder finder = new GeoDataFinder(TimeZones, CSVFormat.TDF, TimeZones.getHeaderNames())) {
                    List<CSVRecord> records = new ArrayList<>();
                    for (CSVRecord record : finder.getRecords()) records.add(record);
                    setTotalTasks(records.size());
                    return records;
                } catch (Exception e) {
                    log.error("Error loading time zones", e);
                    throw new RuntimeException("Error loading time zones", e);
                }
            }).chain(records -> {
                Uni<Void> chain = Uni.createFrom().voidItem();
                for (CSVRecord record : records)
                    chain = chain.chain(() -> {
                        GeographyTimezone timezone = new GeographyTimezone();
                        timezone.setTimezoneID(record.get(1));
                        timezone.setOffsetJan2016(Double.parseDouble(record.get(2)));
                        timezone.setOffsetJuly2016(Double.parseDouble(record.get(3)));
                        timezone.setRawOffset(Double.parseDouble(record.get(4)));
                        return createTimezone(session, timezone, system, identityToken)
                                .chain(() -> countryService.findCountry(session, record.get(0), system))
                                .chain(country -> timeZoneService
                                        .findTimeZone(session, timezone.getTimezoneID(), system, identityToken)
                                        .chain(timeZone -> country.addOrUpdateClassification(session,
                                                                                             TimeZone,
                                                                                             timeZone.getName(),
                                                                                             timeZone.getName(),
                                                                                             system,
                                                                                             identityToken)))
                                .invoke(() -> logProgress("TimeZones",
                                                          "Loaded Timezone - " + timezone.getTimezoneID(),
                                                          1)).replaceWithVoid();
                    });
                return chain;
            }).onFailure().invoke(e -> log.error("Error loading time zones: {}", e.getMessage(), e));
        });
    }

    public Uni<GeographyTimezone> createTimezone(Mutiny.StatelessSession session,
                                                 GeographyTimezone timezone,
                                                 ISystems<?, ?> system,
                                                 UUID... identityToken) {
        return timeZoneService.createTimeZone(session,
                                              timezone.getTimezoneID(),
                                              timezone.getTimezoneID(),
                                              null,
                                              system,
                                              identityToken).chain(() -> timeZoneService.updateTimeZone(session,
                                                                                                        timezone.getTimezoneID(),
                                                                                                        null,
                                                                                                        timezone.getRawOffset() + "",
                                                                                                        timezone.getOffsetJuly2016() + "",
                                                                                                        timezone.getOffsetJan2016() + "",
                                                                                                        system,
                                                                                                        identityToken))
                              .map(updated -> timezone);
    }

    @Override
    public Uni<Void> loadPostalCodes(Mutiny.StatelessSession session,
                                     ISystems<?, ?> requestingSystem,
                                     UUID... requestingToken) {
        return withGeographySystemContext(session,
                                          requestingSystem,
                                          (system, identityToken) -> Uni.createFrom().item(() -> {
                                                                            Map<Long, List<GeographyPostalCode>> postalCodeMap = new HashMap<>();
                                                                            setCurrentTask(0);
                                                                            try (GeoDataFinder finder = new GeoDataFinder(ZAPostalCodes,
                                                                                                                          CSVFormat.TDF,
                                                                                                                          ZAPostalCodes.getHeaderNames())) {
                                                                                for (CSVRecord record : finder.getRecords()) {
                                                                                    GeographyPostalCode post = new GeographyPostalCode();
                                                                                    post.setPostalCode(record.get(1));
                                                                                    post.setPostalCodePlaceName(record.get(2));
                                                                                    post.setParentPlaceName(record.get(2));
                                                                                    post.setCoordinates(new GeographyCoordinates(record.get(9),
                                                                                                                                 record.get(10)));
                                                                                    if (!postalCodeMap.containsKey(Long.valueOf(post.getPostalCode())))
                                                                                        postalCodeMap.put(Long.valueOf(post.getPostalCode()),
                                                                                                          new ArrayList<>());
                                                                                    if (record.size() > 11) {
                                                                                        String t = record.get(11);
                                                                                        if (!Strings.isNullOrEmpty(t))
                                                                                            postalCodeMap.get(Long.valueOf(post.getPostalCode()))
                                                                                                         .add(post);
                                                                                    }
                                                                                }
                                                                            } catch (Exception e) {
                                                                                log.error("Error loading postal codes", e);
                                                                                throw new RuntimeException("Error loading postal codes", e);
                                                                            }
                                                                            return postalCodeMap;
                                                                        }).chain(postalCodeMap -> loadPostalCodeMapStateless(session,
                                                                                                                             postalCodeMap,
                                                                                                                             system,
                                                                                                                             identityToken))
                                                                        .onFailure().invoke(e -> log.error(
                                                          "Error loading postal codes: {}",
                                                          e.getMessage(),
                                                          e)));
    }

    private Uni<Void> loadPostalCodeMapStateless(Mutiny.StatelessSession session,
                                                 Map<Long, List<GeographyPostalCode>> postalCodeMap,
                                                 ISystems<?, ?> system,
                                                 UUID... identityToken) {
        Uni<Void> chain = Uni.createFrom().voidItem();
        setTotalTasks(postalCodeMap.size());
        setCurrentTask(0);
        for (Map.Entry<Long, List<GeographyPostalCode>> entry : postalCodeMap.entrySet()) {
            Long key = entry.getKey();
            List<GeographyPostalCode> value = entry.getValue();
            chain = chain.chain(() -> {
                if (value.isEmpty()) {
                    log.warn("Unknown Postal Code for district? - {}", key);
                    return Uni.createFrom().voidItem();
                }
                GeographyPostalCode gp = value.get(0);
                return townService.findTown(session, gp.getParentPlaceName(), system, identityToken).onFailure()
                                  .recoverWithUni(error -> {
                                      String province = Strings.isNullOrEmpty(gp.getProvinceCode())
                                              ? gp.getProvinceName() : gp.getProvinceCode();
                                      if (!Strings.isNullOrEmpty(province)) return districtService
                                              .findFirstDistrictInProvince(session,
                                                                           province,
                                                                           system,
                                                                           identityToken)
                                              .chain(fd -> fd == null ? Uni.createFrom()
                                                                           .nullItem() : townService.createTown(session,
                                                                                                                fd,
                                                                                                                gp.getParentPlaceName(),
                                                                                                                gp.getParentPlaceName(),
                                                                                                                gp.getGeonameId() == null ? "" : Long.toString(
                                                                                                                        gp.getGeonameId()),
                                                                                                                system,
                                                                                                                identityToken));
                                      return Uni.createFrom().nullItem();
                                  }).chain(town -> {
                            if (town == null) return Uni.createFrom().voidItem();
                            return postalCodeService.createPostalCode(session,
                                                                      town,
                                                                      gp.getPostalCode(),
                                                                      gp.getPostalCodePlaceName(),
                                                                      gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()),
                                                                      system,
                                                                      identityToken).chain(pc -> {
                                Uni<Void> sub = Uni.createFrom().voidItem();
                                for (GeographyPostalCode g : value)
                                    sub = sub.chain(() -> postalCodeService.createPostalCodeSuburb(session,
                                                                                                   pc,
                                                                                                   g.getPostalCode(),
                                                                                                   g.getPostalCodePlaceName(),
                                                                                                   g.getPostalCode(),
                                                                                                   system,
                                                                                                   identityToken)
                                                                           .replaceWithVoid());
                                return sub;
                            }).invoke(() -> logProgress("Postal Codes",
                                                        "Loaded PostalCode - " + gp.getPostalCode(),
                                                        1));
                        });
            });
        }
        return chain;
    }

    @Override
    public Uni<GeographyPostalCode> findPostalCode(Mutiny.StatelessSession session,
                                                   GeographyPostalCode postalCode,
                                                   ISystems<?, ?> system,
                                                   UUID... identityToken) {
        return postalCodeService.findPostalCode(session, null, postalCode.getPostalCode(), system, identityToken)
                                .map(geo -> {
                                    GeographyPostalCode r = new GeographyPostalCode();
                                    r.setGeographyId(geo.getId());
                                    r.setPostalCodePlaceName(geo.getDescription());
                                    return r;
                                });
    }

    @Override
    public Uni<GeographyPostalCode> findPostalCodeSuburb(Mutiny.StatelessSession session,
                                                         String code,
                                                         String description,
                                                         ISystems<?, ?> system,
                                                         UUID... identityToken) {
        return postalCodeService.findPostalCodeSuburb(session, code, description, system, identityToken).map(geo -> {
            GeographyPostalCode r = new GeographyPostalCode();
            r.setGeographyId(geo.getId());
            r.setPostalCodePlaceName(geo.getDescription());
            return r;
        });
    }

    @Override
    public Uni<GeographyPostalCode> findOrCreatePostalCodeSuburb(Mutiny.StatelessSession session,
                                                                 String code,
                                                                 String description,
                                                                 ISystems<?, ?> system,
                                                                 UUID... identityToken) {
        return postalCodeService.findOrCreatePostalCodeSuburb(session, code, description, system, identityToken)
                                .map(geo -> {
                                    GeographyPostalCode r = new GeographyPostalCode();
                                    r.setGeographyId(geo.getId());
                                    r.setPostalCodePlaceName(geo.getDescription());
                                    return r;
                                });
    }

    @Override
    public Uni<IGeography<?, ?>> findGeographyById(Mutiny.StatelessSession session,
                                                   UUID geographyID,
                                                   ISystems<?, ?> system,
                                                   UUID... identityToken) {
        // Geography is @Cacheable — hydrating it as a managed entity on a Mutiny.StatelessSession stalls the
        // reactive pipeline (same @Cacheable/stateless family as the ProductType.findType fix). Project the
        // row's own scalars and prep a detached instance instead of a managed hydrate.
        return preppedGeographyByIdStateless(session, geographyID).onItem().ifNull()
                                                                  .failWith(() -> new GeographyException(
                                                                          "Geography not found: " + geographyID))
                                                                  .map(geo -> (IGeography<?, ?>) geo);
    }

    /**
     * Stateless scalar-projection + prep of a {@link Geography} by id — returns a detached instance carrying
     * {@code (id, name, description)} or {@code null} when the row does not exist. Never hydrates the
     * {@code @Cacheable} managed entity on the stateless session.
     */
    private Uni<Geography> preppedGeographyByIdStateless(Mutiny.StatelessSession session, UUID geographyID) {
        return new Geography().builder(session).find(geographyID).selectColumn(Geography_.id)
                              .selectColumn(Geography_.name).selectColumn(Geography_.description).get(Object[].class)
                              .onFailure(NoResultException.class).recoverWithItem((Object[]) null).map(row -> {
                    if (row == null) {
                        return null;
                    }
                    Geography prepped = new Geography((UUID) row[0], (String) row[1], (String) row[2]);
                    prepped.setFake(false);
                    return prepped;
                });
    }

    public Uni<GeographyFeatureCode> create(Mutiny.StatelessSession session,
                                            GeographyFeatureCode featureCode,
                                            ISystems<?, ?> system,
                                            UUID... identityToken) {
        return classificationService.find(session, FeatureCodes.toString(), system, identityToken)
                                    .chain(classification -> classificationService.create(session,
                                                                                          featureCode.getCode(),
                                                                                          featureCode.getDescription(),
                                                                                          FeatureCodes.concept(),
                                                                                          system,
                                                                                          0,
                                                                                          classification,
                                                                                          identityToken))
                                    .map(created -> featureCode);
    }

    @Override
    public Uni<Void> loadFeatureCodes(Mutiny.StatelessSession session,
                                      ISystems<?, ?> requestingSystem,
                                      UUID... requestingToken) {
        return withGeographySystemContext(session, requestingSystem, (system, identityToken) -> {
            setCurrentTask(0);
            return Uni.createFrom().item(() -> {
                try (GeoDataFinder finder = new GeoDataFinder(FeatureCodes_en,
                                                              CSVFormat.TDF,
                                                              FeatureCodes_en.getHeaderNames())) {
                    List<CSVRecord> records = new ArrayList<>();
                    for (CSVRecord record : finder.getRecords()) records.add(record);
                    setTotalTasks(records.size());
                    return records;
                } catch (Exception e) {
                    log.error("Error loading feature codes", e);
                    throw new RuntimeException("Error loading feature codes", e);
                }
            }).chain(records -> {
                Uni<Void> chain = Uni.createFrom().voidItem();
                for (CSVRecord record : records)
                    chain = chain.chain(() -> {
                        GeographyFeatureCode featureCode = new GeographyFeatureCode();
                        featureCode.setCode(record.get("code"));
                        featureCode.setDescription(record.get("description"));
                        return create(session, featureCode, system, identityToken)
                                .chain(fc -> classificationService.find(session,
                                                                        fc.getClassClassification().toString(),
                                                                        system,
                                                                        identityToken))
                                .chain(clazz -> classificationService.find(session,
                                                                           featureCode.getCode(),
                                                                           FeatureCodes.concept(),
                                                                           system,
                                                                           identityToken).chain(fcc -> {
                                    @SuppressWarnings("unchecked") IClassification<Classification, ?> pp = (IClassification<Classification, ?>) clazz;
                                    return pp.addChild(session,
                                                       (Classification) fcc,
                                                       NoClassification.toString(),
                                                       null,
                                                       system,
                                                       identityToken);
                                })).invoke(() -> logProgress("Geography Feature Codes",
                                                             "Loaded Feature Code - " + featureCode.toString(),
                                                             1)).replaceWithVoid();
                    });
                return chain;
            }).onFailure().invoke(e -> log.error("Error loading feature codes: {}", e.getMessage(), e));
        });
    }

    @Override
    public Uni<GeographyFeatureCode> findFeatureCode(Mutiny.StatelessSession session,
                                                     String featureCode,
                                                     ISystems<?, ?> system,
                                                     UUID... identityToken) {
        return findFeatureCodeClassification(session,
                                             featureCode,
                                             system,
                                             identityToken).map(fc -> new GeographyFeatureCode().setCode(fc.getName())
                                                                                                .setDescription(fc.getDescription()));
    }

    @Override
    public Uni<IClassification<?, ?>> findFeatureCodeClassification(Mutiny.StatelessSession session,
                                                                    String featureCode,
                                                                    ISystems<?, ?> system,
                                                                    UUID... identityToken) {
        return classificationService.find(session, featureCode, system, identityToken);
    }

    @Override
    public Uni<Void> loadTownsAndCities(Mutiny.StatelessSession session,
                                        ISystems<?, ?> requestingSystem,
                                        UUID... requestingToken) {
        return withGeographySystemContext(session,
                                          requestingSystem,
                                          (system, identityToken) -> Uni.createFrom().item(() -> {
                                              Map<Long, GeoNameDefaultData<?>> dataMap = new TreeMap<>();
                                              Map<Long, List<Long>> hierarchyMap = new ConcurrentHashMap<>();
                                              setCurrentTask(0);
                                              try (GeoDataFinder finder = new GeoDataFinder(ZAGeoData,
                                                                                            CSVFormat.TDF,
                                                                                            ZAGeoData.getHeaderNames())) {
                                                  for (CSVRecord a : finder.getRecords()) {
                                                      String fc = a.get("feature code");
                                                      if (Strings.isNullOrEmpty(fc)) fc = "Z.UKN";
                                                      else fc = a.get("feature class") + "." + fc;
                                                      GeoNameDefaultData<?> data = new GeoNameDefaultData<>();
                                                      data.setAsciiname(a.get("asciiname"));
                                                      data.setName(a.get("name"));
                                                      data.setGeonameId(Long.parseLong(a.get("geonameid")));
                                                      data.setCoordinates(new GeographyCoordinates(a.get("latitude"),
                                                                                                   a.get("longitude")));
                                                      if (!Strings.isNullOrEmpty(a.get("dem")))
                                                          data.setDem(Integer.parseInt(a.get("dem")));
                                                      if (!Strings.isNullOrEmpty(a.get("elevation")))
                                                          data.setElevation(Integer.parseInt(a.get("elevation")));
                                                      if (!Strings.isNullOrEmpty(a.get("population")))
                                                          data.setPopulation(Integer.parseInt(a.get("population")));
                                                      data.setAdmin3Code(fc);
                                                      dataMap.put(data.getGeonameId(), data);
                                                  }
                                              } catch (Exception e) {
                                                  log.error("Error loading towns and cities data", e);
                                                  throw new RuntimeException("Error loading towns and cities data", e);
                                              }
                                              loadHierarchyInto(hierarchyMap);
                                              setTotalTasks(dataMap.size());
                                              return new Object[]{dataMap, hierarchyMap};
                                          }).chain(data -> {
                                              @SuppressWarnings("unchecked") Map<Long, GeoNameDefaultData<?>> dataMap = (Map<Long, GeoNameDefaultData<?>>) data[0];
                                              @SuppressWarnings("unchecked") Map<Long, List<Long>> hierarchyMap = (Map<Long, List<Long>>) data[1];
                                              hierarchyMap.entrySet().removeIf(en -> !dataMap.containsKey(en.getKey()));
                                              return districtService.findAllDistricts(session, system, identityToken)
                                                                    .chain(allDistricts -> {
                                                                        Uni<Void> chain = Uni.createFrom().voidItem();
                                                                        for (Geography ig : allDistricts)
                                                                            chain = chain.chain(() -> ig
                                                                                    .findClassification(session,
                                                                                                        GeoNameID,
                                                                                                        system,
                                                                                                        identityToken)
                                                                                    .chain(rel -> {
                                                                                        String s = rel != null ? rel.getValue() : null;
                                                                                        if (Strings.isNullOrEmpty(s))
                                                                                            return Uni.createFrom()
                                                                                                      .voidItem();
                                                                                        List<Long> children = hierarchyMap.get(
                                                                                                Long.valueOf(s));
                                                                                        if (children == null)
                                                                                            return Uni.createFrom()
                                                                                                      .voidItem();
                                                                                        Uni<Void> cc = Uni.createFrom()
                                                                                                          .voidItem();
                                                                                        for (Long childId : children) {
                                                                                            GeoNameDefaultData<?> cd = dataMap.get(
                                                                                                    childId);
                                                                                            if (cd == null) continue;
                                                                                            cc = cc.chain(() -> classificationService
                                                                                                    .find(session,
                                                                                                          Town.toString(),
                                                                                                          system,
                                                                                                          identityToken)
                                                                                                    .chain(tc -> createGeoData(
                                                                                                            session,
                                                                                                            cd,
                                                                                                            tc,
                                                                                                            system,
                                                                                                            identityToken))
                                                                                                    .chain(created -> {
                                                                                                        if (created.getGeographyId() == null)
                                                                                                            return Uni
                                                                                                                    .createFrom()
                                                                                                                    .voidItem();
                                                                                                        return preppedGeographyByIdStateless(
                                                                                                                session,
                                                                                                                created.getGeographyId()).chain(
                                                                                                                nc -> nc == null ? Uni
                                                                                                                                   .createFrom()
                                                                                                                                   .voidItem() : ig
                                                                                                                                                 .addChild(
                                                                                                                                                         session,
                                                                                                                                                         nc,
                                                                                                                                                         NoClassification.toString(),
                                                                                                                                                         STRING_EMPTY,
                                                                                                                                                         system,
                                                                                                                                                         identityToken)
                                                                                                                                                 .replaceWithVoid());
                                                                                                    })
                                                                                                    .invoke(() -> logProgress(
                                                                                                            "Geography Towns/Cities",
                                                                                                            "Loaded place - " + cd.getName(),
                                                                                                            1)));
                                                                                        }
                                                                                        return cc;
                                                                                    }));
                                                                        return chain;
                                                                    });
                                          }).onFailure().invoke(e -> log.error("Error loading towns and cities: {}",
                                                                               e.getMessage(),
                                                                               e)));
    }

    private Uni<GeoNameDefaultData<?>> createGeoData(Mutiny.StatelessSession session,
                                                     GeoNameDefaultData<?> geoData,
                                                     IClassification<?, ?> classification,
                                                     ISystems<?, ?> system,
                                                     UUID... identityToken) {
        if (geoData.getGeonameId() == null) geoData.setGeonameId(-1L);
        var enterprise = system.getEnterprise();
        Geography geo = new Geography();
        return geo.builder(session).withGeoNameID(geoData.getGeonameId().toString()).getCount().chain(count -> {
            if (count > 0) return Uni.createFrom().item(geoData);
            geo.setId(UUID.randomUUID());
            geo.setName(geoData.getName());
            geo.setDescription(geoData.getAsciiname());
            geo.setEnterpriseID(enterprise);
            geo.setClassificationID((Classification) classification);
            geo.setSystemID(system);
            geo.setOriginalSourceSystemID(system.getId());
            IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
            return acService.getActiveFlag(session, enterprise, identityToken).chain(activeFlag -> {
                geo.setActiveFlagID(activeFlag);
                return session.insert(geo).replaceWith(geo);
            }).chain(persisted -> {
                geoData.setGeographyId(geo.getId());
                securityCollector.record(session, geo);
                Uni<?> sc = Uni.createFrom().voidItem();
                if (geoData.getGeonameId() != null && geoData.getGeonameId() != -1L)
                    sc = sc.chain(() -> geo.addClassification(session,
                                                              GeoNameID.toString(),
                                                              Long.toString(geoData.getGeonameId()),
                                                              system,
                                                              identityToken));
                return sc;
            }).chain(r -> {
                Uni<?> cc = Uni.createFrom().voidItem();
                if (geoData.getCoordinates() != null) cc = cc.chain(() -> geo.addClassification(session,
                                                                                                Latitude.toString(),
                                                                                                geoData.getCoordinates()
                                                                                                       .getLatitude(),
                                                                                                system,
                                                                                                identityToken))
                                                             .chain(() -> geo.addClassification(session,
                                                                                                Longitude.toString(),
                                                                                                geoData.getCoordinates()
                                                                                                       .getLongitude(),
                                                                                                system,
                                                                                                identityToken));
                if (geoData.getPopulation() != 0) cc = cc.chain(() -> geo.addClassification(session,
                                                                                            Population.toString(),
                                                                                            Integer.toString(geoData.getPopulation()),
                                                                                            system,
                                                                                            identityToken));
                if (geoData.getElevation() != 0) cc = cc.chain(() -> geo.addClassification(session,
                                                                                           Elevation.toString(),
                                                                                           Integer.toString(geoData.getElevation()),
                                                                                           system,
                                                                                           identityToken));
                if (geoData.getDem() != 0) cc = cc.chain(() -> geo.addClassification(session,
                                                                                     DEM.toString(),
                                                                                     Integer.toString(geoData.getDem()),
                                                                                     system,
                                                                                     identityToken));
                return cc.replaceWith(geoData);
            });
        });
    }

    @Override
    public Uni<Void> installCountry(Mutiny.StatelessSession session,
                                    ISystems<?, ?> system,
                                    String countryCode,
                                    UUID... identityToken) {
        String cc = countryCode.toUpperCase();
        return ensureCountryFilesDownloaded(cc).chain(() -> loadProvincesASCII1(session, system, cc, identityToken))
                                               .chain(() -> loadDistrictsASCII2(session, system, cc, identityToken))
                                               .chain(() -> loadCountryGeoData(session, system, cc, identityToken))
                                               .chain(() -> loadCountryPostalCodes(session, system, cc, identityToken))
                                               .invoke(() -> log.info("Finished installing country {}", cc)).onFailure()
                                               .invoke(e -> log.error("Error installing country {}: {}",
                                                                      cc,
                                                                      e.getMessage(),
                                                                      e));
    }

    @Override
    public Uni<Void> loadCountryGeoData(Mutiny.StatelessSession session,
                                        ISystems<?, ?> requestingSystem,
                                        String countryCode,
                                        UUID... requestingToken) {
        String cc = countryCode.toUpperCase();
        Path geoDataFile = GeoDataLocation.countryGeoDataFile(cc);
        return withGeographySystemContext(session, requestingSystem, (system, identityToken) -> {
            setCurrentTask(0);
            securityCollector.activate(session);
            return Uni.createFrom().item(() -> {
                          Map<Long, GeoNameDefaultData<?>> dataMap = new TreeMap<>();
                          Map<Long, List<Long>> hierarchyMap = new ConcurrentHashMap<>();
                          if (!Files.exists(geoDataFile))
                              throw new GeographyException("GeoNames geo-data file not found for " + cc);
                          loadHierarchyInto(hierarchyMap);
                          Set<Long> needed = new HashSet<>();
                          try (var districts = Files.lines(GeoDataLocation.dumpDirectory().resolve("admin2Codes.txt"))) {
                              districts.filter(line -> line.startsWith(cc + ".")).forEach(line -> {
                                  String[] fields = line.split("\t", -1);
                                  long parent = Long.parseLong(fields[3]);
                                  needed.add(parent);
                                  needed.addAll(hierarchyMap.getOrDefault(parent, List.of()));
                              });
                          } catch (IOException e) {
                              throw new RuntimeException("Error reading published districts for " + cc, e);
                          }
                          try (GeoDataFinder finder = new GeoDataFinder(geoDataFile, CSVFormat.TDF, ZAGeoData.getHeaderNames())) {
                              for (CSVRecord a : finder.getRecords()) {
                                  long source = Long.parseLong(a.get("geonameid"));
                                  if (!needed.contains(source)) continue;
                                  String fc = a.get("feature code");
                                  if (Strings.isNullOrEmpty(fc)) fc = "Z.UKN";
                                  else fc = a.get("feature class") + "." + fc;
                                  GeoNameDefaultData<?> data = new GeoNameDefaultData<>();
                                  data.setAsciiname(a.get("asciiname"));
                                  data.setName(a.get("name"));
                                  data.setGeonameId(source);
                                  data.setCoordinates(new GeographyCoordinates(a.get("latitude"), a.get("longitude")));
                                  if (!Strings.isNullOrEmpty(a.get("dem"))) data.setDem(Integer.parseInt(a.get("dem")));
                                  if (!Strings.isNullOrEmpty(a.get("elevation")))
                                      data.setElevation(Integer.parseInt(a.get("elevation")));
                                  if (!Strings.isNullOrEmpty(a.get("population")))
                                      data.setPopulation(Integer.parseInt(a.get("population")));
                                  data.setAdmin3Code(fc);
                                  dataMap.put(data.getGeonameId(), data);
                              }
                          } catch (GeographyException ge) {
                              throw ge;
                          } catch (Exception e) {
                              throw new RuntimeException("Error loading geo-data for " + cc, e);
                          }
                          setTotalTasks(dataMap.size());
                          return new Object[]{dataMap, hierarchyMap};
                      }).chain(data -> {
                          @SuppressWarnings("unchecked") Map<Long, GeoNameDefaultData<?>> dataMap = (Map<Long, GeoNameDefaultData<?>>) data[0];
                          @SuppressWarnings("unchecked") Map<Long, List<Long>> hierarchyMap = (Map<Long, List<Long>>) data[1];
                          hierarchyMap.entrySet().removeIf(en -> !dataMap.containsKey(en.getKey()));
                          return GeographyBulkWriter.resolve(session, system, classificationService,
                                  IGuiceContext.get(IActiveFlagService.class),
                                  IGuiceContext.get(com.guicedee.activitymaster.fsdm.client.services.ISecurityTokenService.class), identityToken)
                                  .chain(writer -> writer.load(session, system, dataMap, hierarchyMap,
                                          this::setTotalTasks,
                                          count -> logProgress("Geography Geo-Data", "Loaded place batch for " + cc, count)));
                      }).chain(() -> securityCollector.flush(session, system, identityToken))
                      .invoke(() -> logProgress("Geography Geo-Data", "Finished loading geo-data for " + cc))
                      .onFailure().invoke(e -> log.error("Error loading geo-data for {}: {}", cc, e.getMessage(), e));
        });
    }

    @Override
    public Uni<Void> loadCountryPostalCodes(Mutiny.StatelessSession session,
                                            ISystems<?, ?> requestingSystem,
                                            String countryCode,
                                            UUID... requestingToken) {
        String cc = countryCode.toUpperCase();
        Path postalCodeFile = GeoDataLocation.countryPostalCodeFile(cc);
        return withGeographySystemContext(session, requestingSystem, (system, identityToken) -> {
            securityCollector.activate(session);
            return Uni.createFrom().item(() -> {
                          Map<Long, List<GeographyPostalCode>> postalCodeMap = new HashMap<>();
                          setCurrentTask(0);
                          if (!Files.exists(postalCodeFile))
                              throw new GeographyException("GeoNames postal-code file not found for " + cc);
                          PostalProvinceResolver provinces = new PostalProvinceResolver(cc);
                          try (GeoDataFinder finder = new GeoDataFinder(postalCodeFile,
                                                                        CSVFormat.TDF,
                                                                        ZAPostalCodes.getHeaderNames())) {
                              for (CSVRecord record : finder.getRecords()) {
                                  String pcv = record.get(1);
                                  if (Strings.isNullOrEmpty(pcv)) continue;
                                  Long key;
                                  try {
                                      key = Long.valueOf(pcv);
                                  } catch (NumberFormatException nfe) {
                                      continue;
                                  }
                                  GeographyPostalCode post = new GeographyPostalCode();
                                  post.setPostalCode(pcv);
                                  post.setPostalCodePlaceName(record.get(2));
                                  post.setParentPlaceName(record.get(2));
                                  post.setCoordinates(new GeographyCoordinates(record.get(9), record.get(10)));
                                  if (record.size() > 3 && !Strings.isNullOrEmpty(record.get(3)))
                                      post.setProvinceName(record.get(3));
                                  if (record.size() > 4)
                                      post.setProvinceCode(provinces.resolve(record.get(4), post.getProvinceName(), post.getParentPlaceName()));
                                  postalCodeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(post);
                              }
                          } catch (GeographyException ge) {
                              throw ge;
                          } catch (Exception e) {
                              throw new RuntimeException("Error loading postal codes for " + cc, e);
                          }
                          return postalCodeMap;
                      }).chain(postalCodeMap -> GeographyBulkWriter.resolve(session, system, classificationService,
                              IGuiceContext.get(IActiveFlagService.class), IGuiceContext.get(com.guicedee.activitymaster.fsdm.client.services.ISecurityTokenService.class), identityToken)
                              .chain(writer -> writer.loadPostal(session, system, postalCodeMap, this::setTotalTasks,
                                      n -> logProgress("Postal Codes", "Loaded postal batch for " + cc, n))))
                      .chain(() -> securityCollector.flush(session, system, identityToken))
                      .invoke(() -> logProgress("Postal Codes", "Finished loading postal codes for " + cc)).onFailure()
                      .invoke(e -> log.error("Error loading postal codes for {}: {}", cc, e.getMessage(), e));
        });
    }
}
