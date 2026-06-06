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
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

@Log4j2
@Singleton
public class PlanetService
{
	@Inject
	private IClassificationService<?> classificationService;

	@Inject
	private GeographySecurityCollector securityCollector;


	public Uni<IGeography<?, ?>> createPlanet(Mutiny.Session session, String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		// Operate on the caller's session/transaction so writes made earlier in the same
		// install transaction (e.g. the Planet classification) are visible. Opening a nested
		// SessionUtils.withActivityMaster here would start a separate transaction that cannot
		// see those still-uncommitted rows, causing NoResultException on the lookups below.
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, Planet.toString(), createSystem, createIdentityToken)
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
								return findPlanet(createSession, code, createSystem, createIdentityToken);
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
									Uni<?> setupChain = Uni.createFrom().voidItem();
									if (originalUniqueID != null)
									{
										setupChain = setupChain.chain(() -> geo.addClassification(createSession, GeoNameID.toString(), originalUniqueID, createSystem, createIdentityToken));
									}
									return setupChain.map(result -> (IGeography<?, ?>) geo);
								});
						});
				});
	}

	public Uni<IGeography<?, ?>> findPlanet(Mutiny.Session session, String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, Planet.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withClassification((Classification) classification)
						.withName(code)
						.inDateRange()
						.withEnterprise(createEnterprise)
						.inActiveRange()
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Unable to find planet"))
						.map(geo -> (IGeography<?, ?>) geo);
				});
	}
}
