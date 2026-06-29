package com.guicedee.activitymaster.geography;

/**
 * Reactivity Migration Checklist:
 *
 * [✓] One action per Mutiny.Session at a time
 * [✓] Pass Mutiny.Session through the chain
 * [✓] No await() usage
 * [✓] No parallel operations on a session
 * [✓] No session/transaction creation in libraries
 */

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.guicedee.activitymaster.fsdm.client.services.IActiveFlagService;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

@Log4j2
@Singleton
public class ContinentService
{
	// Stateless cache of the stable "Continent" type classification (detached prepped), keyed by enterpriseId.
	// Resolved via the stateless classificationService.find (detached scalar-prepped), safe to reuse; cached on hit.
	private static final java.util.Map<UUID, Classification> CONTINENT_TYPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	@Inject
	private IClassificationService<?> classificationService;

	@Inject
	private GeographySecurityCollector securityCollector;

	@Inject
	private GeographyScopeTokenService scopeTokenService;


	public Uni<IGeography<?, ?>> createContinent(Mutiny.Session session, IGeography<?, ?> planet, String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		// Use the caller's session/transaction so earlier writes in the same install
		// transaction remain visible (a nested withActivityMaster would not see them).
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, Continent.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					Geography geo = new Geography();
					return geo.builder(createSession)
						.withClassification((Classification) classification)
						.withName(code)
						.inDateRange()
						.inActiveRange()
						.withEnterprise(createEnterprise)
						.getCount()
						.chain(count -> {
							if (count > 0)
							{
								return findContinent(createSession, code, createSystem, createIdentityToken);
							}

							geo.setEnterpriseID(createEnterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(createSystem);
							geo.setOriginalSourceSystemID(createSystem.getId());
							geo.setName(code);
							geo.setDescription(description);

							IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
							return acService.getActiveFlag(createSession, createEnterprise, createIdentityToken)
								.chain(activeFlag -> {
									geo.setActiveFlagID(activeFlag);
									return createSession.persist(geo).replaceWith(Uni.createFrom().item(geo));
								})
								.chain(persisted -> {
									securityCollector.record(createSession, geo);
									// Shadow this node into the token graph under its planet's scope token.
									Uni<?> setupChain = scopeTokenService.ensureScope(createSession, geo, planet, description, createSystem, createIdentityToken);
									if (originalUniqueID != null)
									{
										setupChain = setupChain.chain(() -> geo.addClassification(createSession, GeoNameID.toString(), originalUniqueID, createSystem, createIdentityToken));
									}
									return setupChain.chain(() -> planet.addChild(createSession, geo, NoClassification.toString(), null, createSystem, createIdentityToken)
										.replaceWith((IGeography<?, ?>) geo));
								});
						});
				});
	}

	public Uni<IGeography<?, ?>> findContinent(Mutiny.Session session, String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, Continent.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withClassification((Classification) classification)
						.withName(code)
						.inDateRange()
						.inActiveRange()
						.withEnterprise(createEnterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find continent"))
						.map(geo -> (IGeography<?, ?>) geo);
				});
	}

	// ---- Stateless twins ----

	public Uni<IGeography<?, ?>> createContinent(Mutiny.StatelessSession session, IGeography<?, ?> planet, String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return classificationService.find(session, Continent.toString(), system, identityToken)
				.chain(classification -> new Geography().builder(session)
						.withClassification((Classification) classification).withName(code).inDateRange().inActiveRange()
						.withEnterprise(enterprise).getCount()
						.chain(count -> {
							if (count > 0) return findContinent(session, code, system, identityToken);
							Geography geo = new Geography();
							geo.setId(UUID.randomUUID());
							geo.setEnterpriseID(enterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(system);
							geo.setOriginalSourceSystemID(system.getId());
							geo.setName(code);
							geo.setDescription(description);
							IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
							return acService.getActiveFlag(session, enterprise, identityToken)
								.chain(activeFlag -> { geo.setActiveFlagID(activeFlag); return session.insert(geo).replaceWith(geo); })
								.chain(persisted -> {
									securityCollector.record(session, geo);
									Uni<?> setupChain = scopeTokenService.ensureScope(session, geo, planet, description, system, identityToken);
									if (originalUniqueID != null)
										setupChain = setupChain.chain(() -> geo.addClassification(session, GeoNameID.toString(), originalUniqueID, system, identityToken));
									return setupChain.chain(() -> planet.addChild(session, geo, NoClassification.toString(), null, system, identityToken).replaceWith((IGeography<?, ?>) geo));
								});
						}));
	}

	public Uni<IGeography<?, ?>> findContinent(Mutiny.StatelessSession session, String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		UUID enterpriseId = enterprise.getId();
		Classification cachedType = CONTINENT_TYPE_CACHE.get(enterpriseId);
		Uni<Classification> typeUni = cachedType != null
				? Uni.createFrom().item(cachedType)
				: classificationService.find(session, Continent.toString(), system, identityToken)
						.map(c -> (Classification) (Object) c)
						.onItem().invoke(c -> { if (c != null && c.getId() != null) CONTINENT_TYPE_CACHE.put(enterpriseId, c); });
		return typeUni
				.chain(classification -> new Geography().builder(session)
						.withClassification(classification).withName(code).inDateRange().inActiveRange().withEnterprise(enterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find continent"))
						.map(geo -> (IGeography<?, ?>) geo));
	}
}
