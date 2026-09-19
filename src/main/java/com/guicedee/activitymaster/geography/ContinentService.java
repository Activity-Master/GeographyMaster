package com.guicedee.activitymaster.geography;

/**
 * Reactivity Migration Checklist:
 *
 * [✓] One action per Mutiny.StatelessSession at a time
 * [✓] Pass Mutiny.StatelessSession through the chain
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
	@Inject
	private IClassificationService<?> classificationService;

	@Inject
	private GeographySecurityCollector securityCollector;

	@Inject
	private GeographyScopeTokenService scopeTokenService;


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
		// A type read during installation may still roll back. Resolve it in the caller's transaction.
		return classificationService.find(session, Continent.toString(), system, identityToken)
				.map(c -> (Classification) c)
				.chain(classification -> new Geography().builder(session)
						.withClassification(classification).withName(code).inDateRange().inActiveRange().withEnterprise(enterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find continent"))
						.map(geo -> (IGeography<?, ?>) geo));
	}
}
