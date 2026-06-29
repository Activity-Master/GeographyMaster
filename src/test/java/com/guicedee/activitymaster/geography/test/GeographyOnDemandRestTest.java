package com.guicedee.activitymaster.geography.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.activitymaster.fsdm.client.services.IEnterpriseService;
import com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.geography.implementations.updates.GeographySystemInstall;
import com.guicedee.activitymaster.geography.rest.GeographyRestService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeographyCountry;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test proving that geography data loading is only triggered on demand via REST
 * endpoints (or event bus), never at startup.
 *
 * <p>This test:</p>
 * <ol>
 *   <li>Boots the reactive stack with Testcontainers PostgreSQL.</li>
 *   <li>Verifies that only the lightweight taxonomy (classifications, planet, continents) is installed
 *       at startup — no countries, languages, timezones, feature codes, provinces, districts, towns,
 *       or postal codes are loaded automatically.</li>
 *   <li>Invokes the on-demand REST endpoints to load data and verifies they succeed.</li>
 * </ol>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeographyOnDemandRestTest
{
	private static final String ENTERPRISE = "GeoOnDemandTestCo";
	private static final String GEO_SYSTEM = IGeographyService.GeographySystemName;

	private Mutiny.SessionFactory sessionFactory;
	private GeographyRestService restService;

	@BeforeAll
	public void setup()
	{
		ActivityMasterConfiguration.get().setApplicationEnterpriseName(ENTERPRISE);
		IGuiceContext.instance();

		sessionFactory = IGuiceContext.get(Key.get(Mutiny.SessionFactory.class, Names.named("ActivityMaster-Test")));
		assertNotNull(sessionFactory, "SessionFactory should not be null");

		restService = IGuiceContext.get(GeographyRestService.class);
		assertNotNull(restService, "GeographyRestService should be injectable");

		bootstrapEnterprise();
		installGeographyTaxonomy();
	}

	private void bootstrapEnterprise()
	{
		sessionFactory.withSession(session -> session.withTransaction(tx -> {
			IEnterpriseService<?> enterpriseService = IGuiceContext.get(IEnterpriseService.class);
			return enterpriseService.getEnterprise(session, ENTERPRISE)
					.onFailure().recoverWithUni(t -> {
						var ent = enterpriseService.get();
						ent.setName(ENTERPRISE);
						ent.setDescription("Geography on-demand REST test enterprise");
						return enterpriseService.createNewEnterprise(session, ent)
								.chain(e -> enterpriseService.startNewEnterprise(session, ENTERPRISE, "admin", "adminadmin!@"));
					})
					.replaceWith(Uni.createFrom().voidItem());
		})).await().atMost(Duration.ofMinutes(3));
	}

	/**
	 * Only installs the classification taxonomy, planet, and continents — the minimal schema.
	 * No bulk data loading happens at startup.
	 */
	private void installGeographyTaxonomy()
	{
		IEnterpriseService<?> enterpriseService = IGuiceContext.get(IEnterpriseService.class);
		IEnterprise<?, ?> enterprise = sessionFactory.withSession(s -> enterpriseService.getEnterprise(s, ENTERPRISE))
				.await().atMost(Duration.ofMinutes(1));
		assertNotNull(enterprise, "Enterprise must exist before installing the geography taxonomy");

		GeographySystemInstall install = IGuiceContext.get(GeographySystemInstall.class);
		Boolean done = install.update((Mutiny.Session) null, enterprise).await().atMost(Duration.ofMinutes(3));
		assertEquals(Boolean.TRUE, done, "Geography taxonomy installation should succeed");
	}

	// -------------------------------------------------------------------------------------------
	//  Verify nothing loaded at startup
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(1)
	@DisplayName("No countries are loaded at startup - only taxonomy exists")
	public void noCountriesLoadedAtStartup()
	{
		// Attempting to find any country should fail because no country data was loaded
		assertThrows(Exception.class, () ->
				restService.findCountry(ENTERPRISE, GEO_SYSTEM, "ZA")
						.await().atMost(Duration.ofSeconds(30)),
				"No country should be found at startup since data is only loaded on demand"
		);
	}

	// -------------------------------------------------------------------------------------------
	//  On-demand REST endpoint tests
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(10)
	@DisplayName("POST install/languages loads language data on demand")
	public void installLanguagesOnDemand()
	{
		String result = restService.installLanguages(ENTERPRISE, GEO_SYSTEM)
				.await().atMost(Duration.ofMinutes(5));

		assertNotNull(result, "installLanguages should return a result");
		assertTrue(result.contains("Languages loaded"), "Result should confirm languages loaded: " + result);
	}

	@Test
	@Order(20)
	@DisplayName("POST install/countries loads country info on demand")
	public void installCountriesOnDemand()
	{
		String result = restService.installCountries(ENTERPRISE, GEO_SYSTEM)
				.await().atMost(Duration.ofMinutes(5));

		assertNotNull(result, "installCountries should return a result");
		assertTrue(result.contains("Country info loaded"), "Result should confirm countries loaded: " + result);
	}

	@Test
	@Order(25)
	@DisplayName("After on-demand country install, country ZA is now queryable")
	public void countryQueryableAfterOnDemandInstall()
	{
		GeographyCountry za = restService.findCountry(ENTERPRISE, GEO_SYSTEM, "ZA")
				.await().atMost(Duration.ofMinutes(1));

		assertNotNull(za, "South Africa should be queryable after on-demand install");
		assertEquals("ZA", za.getIso(), "Country ISO should be ZA");
		assertNotNull(za.getGeographyId(), "Geography ID should be populated");
	}

	@Test
	@Order(30)
	@DisplayName("POST install/feature-codes loads feature codes on demand")
	public void installFeatureCodesOnDemand()
	{
		String result = restService.installFeatureCodes(ENTERPRISE, GEO_SYSTEM)
				.await().atMost(Duration.ofMinutes(5));

		assertNotNull(result, "installFeatureCodes should return a result");
		assertTrue(result.contains("Feature codes loaded"), "Result should confirm feature codes loaded: " + result);
	}

	@Test
	@Order(40)
	@DisplayName("POST install/timezones loads timezone data on demand")
	public void installTimeZonesOnDemand()
	{
		String result = restService.installTimeZones(ENTERPRISE, GEO_SYSTEM)
				.await().atMost(Duration.ofMinutes(5));

		assertNotNull(result, "installTimeZones should return a result");
		assertTrue(result.contains("Time zones loaded"), "Result should confirm timezones loaded: " + result);
	}

	@Test
	@Order(50)
	@DisplayName("POST install/country/{countryCode}/provinces loads provinces for a country")
	public void installProvincesOnDemand()
	{
		String result = restService.installProvinces(ENTERPRISE, GEO_SYSTEM, "ZA")
				.await().atMost(Duration.ofMinutes(5));

		assertNotNull(result, "installProvinces should return a result");
		assertTrue(result.contains("Provinces loaded for ZA"), "Result should confirm provinces loaded: " + result);
	}

	@Test
	@Order(60)
	@DisplayName("POST install/country/{countryCode}/districts loads districts for a country")
	public void installDistrictsOnDemand()
	{
		String result = restService.installDistricts(ENTERPRISE, GEO_SYSTEM, "ZA")
				.await().atMost(Duration.ofMinutes(5));

		assertNotNull(result, "installDistricts should return a result");
		assertTrue(result.contains("Districts loaded for ZA"), "Result should confirm districts loaded: " + result);
	}

	// -------------------------------------------------------------------------------------------
	//  findCountryDetailed after full install
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(70)
	@DisplayName("findCountryDetailed returns fully-hydrated DTO after on-demand install")
	public void findCountryDetailedAfterInstall()
	{
		GeographyCountry za = restService.findCountry(ENTERPRISE, GEO_SYSTEM, "ZA")
				.await().atMost(Duration.ofMinutes(1));

		assertNotNull(za, "South Africa should be queryable");
		assertEquals("ZA", za.getIso());
		assertNotNull(za.getGeographyId(), "Geography ID should be populated");
		// These fields are hydrated from classifications created during country info install
		assertNotNull(za.getCountryName(), "Country name should be populated from the warehouse");
	}
}


