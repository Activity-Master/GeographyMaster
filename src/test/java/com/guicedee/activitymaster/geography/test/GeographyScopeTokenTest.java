package com.guicedee.activitymaster.geography.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.IEnterpriseService;
import com.guicedee.activitymaster.fsdm.client.services.ISecurityTokenService;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.base.IWarehouseCoreTable;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.security.ISecurityToken;
import com.guicedee.activitymaster.fsdm.client.services.classifications.SecurityTokenClassifications;
import com.guicedee.activitymaster.geography.ContinentService;
import com.guicedee.activitymaster.geography.GeographyScopeTokenService;
import com.guicedee.activitymaster.geography.PlanetService;
import com.guicedee.activitymaster.geography.implementations.updates.GeographySystemInstall;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the <em>geography scope-token</em> feature (Increment 1) and the
 * {@link ISecurityTokenService#moveToken(Mutiny.Session, ISecurityToken, ISecurityToken, ISecurityToken, IClassification, String...) moveToken}
 * capability (Q2).
 *
 * <p>It boots the reactive stack on Testcontainers PostgreSQL, installs the geography taxonomy
 * (Planet "Earth" + continents, which now create their shadow scope tokens), and asserts:</p>
 * <ol>
 *   <li>each geography node has a {@code GeoScope:<id>} token, and the scope tokens are nested
 *       Continent &rarr; Planet &rarr; {@code Everywhere} (proved by the recursive applicable-token
 *       expansion from a continent scope token); and</li>
 *   <li>{@code moveToken} relocates a user group from one parent group to another — the child's
 *       applicable-token set picks up the new parent and drops the old one.</li>
 * </ol>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeographyScopeTokenTest
{
	private static final String ENTERPRISE = "GeoScopeTokenTestCo";
	private static final String GEO_SYSTEM = IGeographyService.GeographySystemName;

	private Mutiny.SessionFactory sessionFactory;

	@BeforeAll
	public void setup()
	{
		ActivityMasterConfiguration.get().setApplicationEnterpriseName(ENTERPRISE);
		IGuiceContext.instance();

		sessionFactory = IGuiceContext.get(Key.get(Mutiny.SessionFactory.class, Names.named("ActivityMaster-Test")));
		assertNotNull(sessionFactory, "SessionFactory should not be null");

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
						ent.setDescription("Geography scope-token test enterprise");
						return enterpriseService.createNewEnterprise(session, ent)
								.chain(e -> enterpriseService.startNewEnterprise(session, ENTERPRISE, "admin", "adminadmin!@"));
					})
					.replaceWith(Uni.createFrom().voidItem());
		})).await().atMost(Duration.ofMinutes(3));
	}

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
	//  Increment 1 — scope tokens exist and are nested Continent → Planet → Everywhere
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(1)
	@DisplayName("Planet and continent get GeoScope tokens nested under Everywhere")
	public void scopeTokensAreCreatedAndNested()
	{
		Object[] result = SessionUtils.<Object[]>withActivityMaster(ENTERPRISE, GEO_SYSTEM, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			var system = tuple.getItem3();
			UUID[] token = tuple.getItem4();

			PlanetService planetService = IGuiceContext.get(PlanetService.class);
			ContinentService continentService = IGuiceContext.get(ContinentService.class);
			GeographyScopeTokenService scopes = IGuiceContext.get(GeographyScopeTokenService.class);
			ISecurityTokenService<?> sec = IGuiceContext.get(ISecurityTokenService.class);

			return planetService.findPlanet(session, "Earth", system, token)
					.chain(earth -> continentService.findContinent(session, "AF", system, token)
							.chain(africa -> scopes.findScope(session, earth, system, token)
									.chain(earthScope -> scopes.findScope(session, africa, system, token)
											.chain(africaScope -> sec.getEverywhereGroup(session, system, token)
													.chain(everywhere -> sec.getApplicableSecurityTokenIds(session, system,
																	UUID.fromString(africaScope.getSecurityToken()))
															.map(applicable -> new Object[]{earthScope, africaScope, everywhere, applicable}))))));
		}).await().atMost(Duration.ofMinutes(2));

		ISecurityToken<?, ?> earthScope = (ISecurityToken<?, ?>) result[0];
		ISecurityToken<?, ?> africaScope = (ISecurityToken<?, ?>) result[1];
		ISecurityToken<?, ?> everywhere = (ISecurityToken<?, ?>) result[2];
		@SuppressWarnings("unchecked")
		Set<UUID> applicable = (Set<UUID>) result[3];

		assertNotNull(earthScope, "Planet 'Earth' should have a GeoScope token");
		assertNotNull(africaScope, "Continent 'AF' should have a GeoScope token");
		assertNotNull(everywhere, "Everywhere group should exist");

		// Expanding the continent scope token must climb Continent → Planet → Everywhere.
		assertTrue(applicable.contains(africaScope.getId()), "Applicable set should include the continent scope itself");
		assertTrue(applicable.contains(earthScope.getId()), "Continent scope should be nested under the planet scope");
		assertTrue(applicable.contains(everywhere.getId()), "Planet scope should be nested under the Everywhere group");
	}

	// -------------------------------------------------------------------------------------------
	//  Q2 — moveToken relocates a user group from one parent group to another
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(2)
	@DisplayName("moveToken moves a user group from group A to group B")
	public void moveTokenRelocatesGroupMembership()
	{
		Object[] result = SessionUtils.<Object[]>withActivityMaster(ENTERPRISE, GEO_SYSTEM, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			var system = tuple.getItem3();

			ISecurityTokenService<?> sec = IGuiceContext.get(ISecurityTokenService.class);
			IClassificationService<?> cls = IGuiceContext.get(IClassificationService.class);
			String ug = SecurityTokenClassifications.UserGroup.toString();

			return cls.find(session, SecurityTokenClassifications.UserGroup, system)
					.chain(userGroupClass -> sec.create(session, ug, "ScopeTestGroupA", "Test group A", system)
							.chain(groupA -> sec.create(session, ug, "ScopeTestGroupB", "Test group B", system)
									.chain(groupB -> sec.create(session, ug, "ScopeTestChildGroup", "Test child group", system)
											.chain(child -> sec.link(session, groupA, child, (IClassification<?, ?>) userGroupClass)
													.chain(() -> sec.getApplicableSecurityTokenIds(session, system,
																	UUID.fromString(child.getSecurityToken())))
													.chain(before -> sec.moveToken(session, groupA, groupB, child, (IClassification<?, ?>) userGroupClass)
															.chain(() -> sec.getApplicableSecurityTokenIds(session, system,
																	UUID.fromString(child.getSecurityToken())))
															.map(after -> new Object[]{groupA.getId(), groupB.getId(), before, after}))))));
		}).await().atMost(Duration.ofMinutes(2));

		UUID groupAId = (UUID) result[0];
		UUID groupBId = (UUID) result[1];
		@SuppressWarnings("unchecked")
		Set<UUID> before = (Set<UUID>) result[2];
		@SuppressWarnings("unchecked")
		Set<UUID> after = (Set<UUID>) result[3];

		assertTrue(before.contains(groupAId), "Before the move, the child should be a member of group A");
		assertTrue(after.contains(groupBId), "After the move, the child should be a member of group B");
		assertFalse(after.contains(groupAId), "After the move, the child should no longer be a member of group A");
	}

	// -------------------------------------------------------------------------------------------
	//  (a) Scope-restricted classification — readable under-scope, not by an outsider
	// -------------------------------------------------------------------------------------------

	@Test
	@Order(3)
	@DisplayName("A scope-restricted classification is readable under its scope but not by an outsider")
	public void scopeRestrictedClassificationIsBranchRestricted()
	{
		Boolean[] result = SessionUtils.<Boolean[]>withActivityMaster(ENTERPRISE, GEO_SYSTEM, tuple -> {
			Mutiny.Session session = tuple.getItem1();
			var system = tuple.getItem3();
			UUID[] token = tuple.getItem4();

			ISecurityTokenService<?> sec = IGuiceContext.get(ISecurityTokenService.class);
			IClassificationService<?> cls = IGuiceContext.get(IClassificationService.class);
			String ug = SecurityTokenClassifications.UserGroup.toString();

			return cls.find(session, SecurityTokenClassifications.UserGroup, system)
					.chain(userGroupClass -> sec.create(session, ug, "RestrictScopeToken", "Scope token", system)
					.chain(scope -> sec.create(session, ug, "RestrictInsider", "Identity under the scope", system)
					.chain(insider -> sec.create(session, ug, "RestrictOutsider", "Identity outside the scope", system)
					.chain(outsider -> sec.link(session, scope, insider, (IClassification<?, ?>) userGroupClass)
					.chain(linked -> cls.createScopeRestricted(session, "RestrictedClassification",
							"A scope-restricted classification", null, system, 0, null, scope, token)
					.chain(restricted -> session.flush()
					.chain(flushed -> ((IWarehouseCoreTable<?, ?, ?, ?>) restricted).canRead(session, system, UUID.fromString(insider.getSecurityToken()))
					.chain(insiderCanRead -> ((IWarehouseCoreTable<?, ?, ?, ?>) restricted).canRead(session, system, UUID.fromString(outsider.getSecurityToken()))
					.map(outsiderCanRead -> new Boolean[]{insiderCanRead, outsiderCanRead})))))))));
		}).await().atMost(Duration.ofMinutes(2));

		boolean insiderCanRead = result[0];
		boolean outsiderCanRead = result[1];

		assertTrue(insiderCanRead, "An identity linked under the scope token should read the restricted classification");
		assertFalse(outsiderCanRead, "An identity outside the scope should NOT read the restricted classification");
	}
}

