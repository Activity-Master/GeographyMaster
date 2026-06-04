package com.guicedee.activitymaster.geography.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.activitymaster.fsdm.client.services.IEnterpriseService;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.geography.GeographyService;
import com.guicedee.activitymaster.geography.implementations.updates.GeographySystemInstall;
import com.guicedee.activitymaster.geography.rest.GeographyRestService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.dto.GeographyCountry;
import com.guicedee.activitymaster.geography.services.dto.GeographyCurrency;
import com.guicedee.client.IGuiceContext;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test proving that the geography module's data structures become available
 * through the shared ActivityMaster service registry purely by being on the class/module path.
 *
 * <p>The test boots the reactive stack against a Testcontainers PostgreSQL instance, installs the
 * geography taxonomy/continents (via {@link GeographySystemInstall} — the lightweight classification
 * setup, not the heavyweight CSV bulk loaders), persists a single country into the ActivityMaster
 * warehouse, and then asserts that:</p>
 * <ul>
 *     <li>the {@code geographyCountry} GraphQL query (contributed by the geography module's
 *         {@code GeographyGraphQLSchemaProvider}) returns the strongly-typed country with correct
 *         field values;</li>
 *     <li>the {@code GeographyRestService} returns the same strongly-typed DTO;</li>
 *     <li>the underlying data persisted onto ActivityMaster is correct.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeographyGraphQLIntegrationTest
{
    private static final String ENTERPRISE = "GeoGraphQLTestCo";
    private static final String GEO_SYSTEM = IGeographyService.GeographySystemName;

    private static final String ISO = "ZZ";
    private static final String ISO3 = "ZZZ";
    private static final String ISO_NUMERIC = "999";
    private static final String FIPS = "ZF";
    private static final String COUNTRY_NAME = "Testlandia";
    private static final String CAPITAL = "Testville";
    private static final String AREA = "12345";
    private static final String TLD = ".zz";
    private static final int POPULATION = 123456;
    private static final String DIAL_CODE = "999";
    private static final long GEONAME_ID = 8888888L;

    private Mutiny.SessionFactory sessionFactory;
    private GraphQL graphQL;

    @BeforeAll
    public void setup()
    {
        ActivityMasterConfiguration.get().setApplicationEnterpriseName(ENTERPRISE);
        IGuiceContext.instance();

        sessionFactory = IGuiceContext.get(Key.get(Mutiny.SessionFactory.class, Names.named("ActivityMaster-Test")));
        assertNotNull(sessionFactory, "SessionFactory should not be null");

        graphQL = IGuiceContext.get(GraphQL.class);
        assertNotNull(graphQL, "GraphQL instance should be assembled from the schema providers");

        bootstrapEnterprise();
        installGeographyTaxonomy();
        createTestCountry();
    }

    /** Creates and starts the enterprise (which also registers the geography system). */
    private void bootstrapEnterprise()
    {
        sessionFactory.withSession(session -> session.withTransaction(tx -> {
            IEnterpriseService<?> enterpriseService = IGuiceContext.get(IEnterpriseService.class);
            return enterpriseService.getEnterprise(session, ENTERPRISE)
                    .onFailure().recoverWithUni(t -> {
                        var ent = enterpriseService.get();
                        ent.setName(ENTERPRISE);
                        ent.setDescription("Geography GraphQL integration-test enterprise");
                        return enterpriseService.createNewEnterprise(session, ent)
                                .chain(e -> enterpriseService.startNewEnterprise(session, ENTERPRISE, "admin", "adminadmin!@"));
                    })
                    .replaceWith(Uni.createFrom().voidItem());
        })).await().atMost(Duration.ofMinutes(3));
    }

    /**
     * Installs only the geography classification taxonomy, planet and continents — the cheap subset
     * required to create a country. The heavyweight CSV loaders (countries, timezones, ZA geodata)
     * are deliberately not run.
     */
    private void installGeographyTaxonomy()
    {
        IEnterpriseService<?> enterpriseService = IGuiceContext.get(IEnterpriseService.class);
        IEnterprise<?, ?> enterprise = sessionFactory.withSession(s -> enterpriseService.getEnterprise(s, ENTERPRISE))
                .await().atMost(Duration.ofMinutes(1));
        assertNotNull(enterprise, "Enterprise must exist before installing the geography taxonomy");

        GeographySystemInstall install = IGuiceContext.get(GeographySystemInstall.class);
        Boolean done = install.update(null, enterprise).await().atMost(Duration.ofMinutes(3));
        assertEquals(Boolean.TRUE, done, "Geography taxonomy installation should succeed");
    }

    /** Persists a single, fully-specified country into the ActivityMaster geography warehouse. */
    private void createTestCountry()
    {
        GeographyContinent continent = new GeographyContinent();
        continent.setContinentCode("AF");
        continent.setContinentName("Africa");

        GeographyCurrency currency = new GeographyCurrency();
        currency.setCurrencyCode("TST");
        currency.setCurrencyName("Test Dollar");

        GeographyCountry country = new GeographyCountry();
        country.setIso(ISO);
        country.setIso3(ISO3);
        country.setIsoNumeric(ISO_NUMERIC);
        country.setFips(FIPS);
        country.setCountryName(COUNTRY_NAME);
        country.setCapital(CAPITAL);
        country.setAreaSqlKM(AREA);
        country.setWebTld(TLD);
        country.setPopulation(POPULATION);
        country.setCountryDialCode(DIAL_CODE);
        country.setPostalCodeDecimalFormat("####");
        country.setPostalCodeRegexFormat("^\\d{4}$");
        country.setContinent(continent);
        country.setCurrency(currency);
        country.setGeonameId(GEONAME_ID);

        SessionUtils.withActivityMaster(ENTERPRISE, GEO_SYSTEM, tuple -> {
            GeographyService geographyService = (GeographyService) IGuiceContext.get(IGeographyService.class);
            return geographyService.createCountry(tuple.getItem1(), country, tuple.getItem3(), tuple.getItem4());
        }).await().atMost(Duration.ofMinutes(2));
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    @Order(1)
    public void geographyCountryQueryReturnsStronglyTypedCountry()
    {
        String document =
                "query Country($e: String!, $s: String!, $iso: String!) {\n"
                + "    geographyCountry(enterprise: $e, system: $s, iso: $iso) {\n"
                + "        geographyId\n"
                + "        geonameId\n"
                + "        iso\n"
                + "        iso3\n"
                + "        isoNumeric\n"
                + "        fips\n"
                + "        countryName\n"
                + "        capital\n"
                + "        areaSqlKM\n"
                + "        webTld\n"
                + "        population\n"
                + "        countryDialCode\n"
                + "        postalCodeDecimalFormat\n"
                + "        postalCodeRegexFormat\n"
                + "    }\n"
                + "}\n";

        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query(document)
                .variables(Map.of("e", ENTERPRISE, "s", GEO_SYSTEM, "iso", ISO))
                .build();

        ExecutionResult result;
        try
        {
            result = graphQL.executeAsync(input).get(2, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            throw new RuntimeException("GraphQL execution failed for geographyCountry", e);
        }

        assertTrue(result.getErrors().isEmpty(), () -> "GraphQL errors: " + result.getErrors());

        Map<String, Object> data = result.getData();
        assertNotNull(data, "GraphQL data should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> country = (Map<String, Object>) data.get("geographyCountry");
        assertNotNull(country, "geographyCountry should resolve a country");

        assertNotNull(country.get("geographyId"), "geographyId should be populated from the warehouse row");
        assertEquals(ISO, country.get("iso"));
        assertEquals(ISO3, country.get("iso3"));
        assertEquals(ISO_NUMERIC, country.get("isoNumeric"));
        assertEquals(FIPS, country.get("fips"));
        assertEquals(COUNTRY_NAME, country.get("countryName"));
        assertEquals(CAPITAL, country.get("capital"));
        assertEquals(AREA, country.get("areaSqlKM"));
        assertEquals(TLD, country.get("webTld"));
        assertEquals(POPULATION, ((Number) country.get("population")).intValue());
        assertEquals(DIAL_CODE, country.get("countryDialCode"));
        assertEquals(String.valueOf(GEONAME_ID), country.get("geonameId"));
    }

    @Test
    @Order(2)
    public void geographyRestServiceReturnsStronglyTypedCountry()
    {
        GeographyRestService restService = IGuiceContext.get(GeographyRestService.class);
        GeographyCountry country = restService.findCountry(ENTERPRISE, GEO_SYSTEM, ISO)
                .await().atMost(Duration.ofMinutes(2));

        assertNotNull(country, "REST resource should resolve the country DTO");
        assertNotNull(country.getGeographyId(), "geographyId should be populated from the warehouse row");
        assertEquals(ISO, country.getIso());
        assertEquals(ISO3, country.getIso3());
        assertEquals(ISO_NUMERIC, country.getIsoNumeric());
        assertEquals(COUNTRY_NAME, country.getCountryName());
        assertEquals(CAPITAL, country.getCapital());
        assertEquals(TLD, country.getWebTld());
        assertEquals(POPULATION, country.getPopulation());
        assertEquals(GEONAME_ID, country.getGeonameId());
    }

    @Test
    @Order(3)
    public void persistedDataMatchesWhatWasStoredOnActivityMaster()
    {
        // Re-read directly through the service to assert the warehouse round-trip is faithful.
        IGeographyService<?> geographyService = IGuiceContext.get(IGeographyService.class);
        GeographyCountry persisted = SessionUtils.withActivityMaster(ENTERPRISE, GEO_SYSTEM, tuple -> {
            return geographyService.findCountryDetailed(tuple.getItem1(), ISO, tuple.getItem3(), tuple.getItem4());
        }).await().atMost(Duration.ofMinutes(2));

        assertNotNull(persisted, "The country must be persisted on ActivityMaster");
        // The warehouse stores the ISO as the Geography name and the country name as its description.
        assertEquals(ISO, persisted.getIso(), "Stored Geography name should be the ISO code");
        assertEquals(COUNTRY_NAME, persisted.getCountryName(), "Stored Geography description should be the country name");
        assertEquals(ISO3, persisted.getIso3());
        assertEquals(ISO_NUMERIC, persisted.getIsoNumeric());
        assertEquals(CAPITAL, persisted.getCapital());
        assertEquals(POPULATION, persisted.getPopulation());
        assertEquals(GEONAME_ID, persisted.getGeonameId());
    }
}




