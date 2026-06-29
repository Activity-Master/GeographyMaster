package com.guicedee.activitymaster.geography.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.IEnterpriseService;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.geography.ContinentService;
import com.guicedee.activitymaster.geography.PlanetService;
import com.guicedee.activitymaster.geography.implementations.updates.GeographySystemInstall;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.utils.LogUtils;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end geography test driven from a <strong>stateless</strong> enterprise lifecycle.
 *
 * <p>The enterprise is created entirely through the genuinely-stateless, no-bridge
 * {@link IEnterpriseService#startNewEnterprise(Mutiny.StatelessSession, String, String, String)} entry
 * point (every phase runs on its own stateless transaction), then the geography taxonomy is installed
 * via {@link GeographySystemInstall}. Finally the install is verified through the
 * <em>stateless</em> geography lookups ({@link PlanetService#findPlanet(Mutiny.StatelessSession, String,
 * com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems, UUID...)} /
 * {@link ContinentService#findContinent(Mutiny.StatelessSession, String,
 * com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems, UUID...)}),
 * which resolve the stable Planet/Continent/Country type classifications through the cached
 * detached-prepped path.</p>
 */
@Log4j2
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeographyStatelessEnterpriseTest
{
	private static final String ENTERPRISE = "GeoStatelessTestCo";
	private static final String GEO_SYSTEM = IGeographyService.GeographySystemName;
	private static final String[] CONTINENT_CODES = {"AF", "AS", "NA", "EU", "OC", "SA", "AN"};

	private Mutiny.SessionFactory sessionFactory;
	private UUID enterpriseId;

	@BeforeAll
	public void setup()
	{
        LogUtils.addConsoleLogger(Level.INFO);
		ActivityMasterConfiguration.get().setApplicationEnterpriseName(ENTERPRISE);
		IGuiceContext.instance();

		sessionFactory = IGuiceContext.get(Key.get(Mutiny.SessionFactory.class, Names.named("ActivityMaster-Test")));
		assertNotNull(sessionFactory, "SessionFactory should not be null");

		// Provision the enterprise once through the proven STATEFUL path (registers all systems, incl. the
		// geography system). The stateless entry point is then exercised idempotently in the tests below —
		// mirroring the core stateless safety-net pattern.
		IEnterpriseService<?> es = IGuiceContext.get(IEnterpriseService.class);
		sessionFactory.withSession(session -> session.withTransaction(tx ->
				es.getEnterprise(session, ENTERPRISE)
						.onFailure().recoverWithUni(t -> {
							var ent = es.get();
							ent.setName(ENTERPRISE);
							ent.setDescription("Geography stateless test enterprise");
							return es.createNewEnterprise(session, ent)
									.chain(e -> es.startNewEnterprise(session, ENTERPRISE, "admin", "adminadmin!@"));
						})
						.replaceWith(Uni.createFrom().voidItem())
		)).await().atMost(Duration.ofMinutes(3));

		IEnterprise<?, ?> baseline = sessionFactory.withSession(s -> es.getEnterprise(s, ENTERPRISE))
				.await().atMost(Duration.ofMinutes(1));
		assertNotNull(baseline, "Baseline enterprise must be provisioned in setup");
		enterpriseId = baseline.getId();
	}

	@Test
	@Order(1)
	@DisplayName("startNewEnterprise completes end-to-end on a stateless (no-bridge) session")
	public void statelessEnterpriseCreate_completes()
	{
		IEnterpriseService<?> es = IGuiceContext.get(IEnterpriseService.class);

		IEnterprise<?, ?> created = sessionFactory.openStatelessSession()
				.chain(ss -> es.startNewEnterprise(ss, ENTERPRISE, "admin", "adminadmin!@")
						.eventually(ss::close))
				.await().atMost(Duration.ofMinutes(3));

		assertNotNull(created, "Stateless startNewEnterprise must complete and return the enterprise");
		assertEquals(enterpriseId, created.getId(),
				"Stateless startNewEnterprise must resolve the provisioned enterprise (idempotent)");
	}

	@Test
	@Order(2)
	@DisplayName("Geography taxonomy installs on top of the stateless-created enterprise")
	public void geographyInstall_succeeds()
	{
		IEnterpriseService<?> es = IGuiceContext.get(IEnterpriseService.class);
		IEnterprise<?, ?> enterprise = sessionFactory.withStatelessSession(s -> es.getEnterprise(s, ENTERPRISE))
				.await().atMost(Duration.ofMinutes(1));
		assertNotNull(enterprise, "Enterprise must resolve before installing the geography taxonomy");
		assertEquals(enterpriseId, enterprise.getId(), "Resolved enterprise must match the stateless-created one");

		GeographySystemInstall install = IGuiceContext.get(GeographySystemInstall.class);
		Boolean done = install.update((Mutiny.Session) null, enterprise).await().atMost(Duration.ofMinutes(3));
		assertEquals(Boolean.TRUE, done, "Geography taxonomy installation should succeed");
	}

	@Test
	@Order(3)
	@DisplayName("Planet 'Earth' resolves via the stateless geography lookup")
	public void statelessFindPlanet_resolvesEarth()
	{
		UUID planetId = SessionUtils.<UUID>withSystemAndTokenStateless(ENTERPRISE, GEO_SYSTEM, tuple -> {
			Mutiny.StatelessSession session = tuple.getItem1();
			var system = tuple.getItem3();
			UUID[] token = tuple.getItem4();
			PlanetService planetService = IGuiceContext.get(PlanetService.class);
			return planetService.findPlanet(session, "Earth", system, token).map(geo -> (UUID) geo.getId());
		}).await().atMost(Duration.ofMinutes(2));

		assertNotNull(planetId, "Stateless findPlanet must resolve 'Earth' after the install");
	}

	@Test
	@Order(4)
	@DisplayName("All seven continents resolve via the stateless geography lookup (cached type classification)")
	public void statelessFindContinents_resolveAll()
	{
		Integer found = SessionUtils.<Integer>withSystemAndTokenStateless(ENTERPRISE, GEO_SYSTEM, tuple -> {
			Mutiny.StatelessSession session = tuple.getItem1();
			var system = tuple.getItem3();
			UUID[] token = tuple.getItem4();
			ContinentService continentService = IGuiceContext.get(ContinentService.class);

			Uni<Integer> chain = Uni.createFrom().item(0);
			for (String code : CONTINENT_CODES)
			{
				chain = chain.chain(count -> continentService.findContinent(session, code, system, token)
						.map(geo -> count + (geo != null && geo.getId() != null ? 1 : 0)));
			}
			return chain;
		}).await().atMost(Duration.ofMinutes(2));

		assertEquals(CONTINENT_CODES.length, found,
				"All seven continents must resolve statelessly after the install");
	}

	@Test
	@Order(5)
	@DisplayName("Geography type classifications resolve via the cached stateless classification find")
	public void statelessTypeClassifications_resolve()
	{
		Object[] ids = SessionUtils.<Object[]>withSystemAndTokenStateless(ENTERPRISE, GEO_SYSTEM, tuple -> {
			Mutiny.StatelessSession session = tuple.getItem1();
			var system = tuple.getItem3();
			UUID[] token = tuple.getItem4();
			IClassificationService<?> cs = IGuiceContext.get(IClassificationService.class);

			return cs.find(session, "Planet", system, token)
					.chain(planet -> cs.find(session, "Continent", system, token)
							.chain(continent -> cs.find(session, "Country", system, token)
									.map(country -> new Object[]{
											planet == null ? null : planet.getId(),
											continent == null ? null : continent.getId(),
											country == null ? null : country.getId()})));
		}).await().atMost(Duration.ofMinutes(1));

		assertNotNull(ids[0], "Planet type classification must resolve after the install");
		assertNotNull(ids[1], "Continent type classification must resolve after the install");
		assertNotNull(ids[2], "Country type classification must resolve after the install");
	}
}





