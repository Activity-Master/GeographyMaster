package com.guicedee.activitymaster.geography;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.guicedee.activitymaster.fsdm.client.services.IActiveFlagService;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
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

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.GeoNameID;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.Planet;

/**
 * Reactivity Migration Checklist:
 * [✓] One action per Mutiny.Session at a time
 * [✓] Pass Mutiny.Session through the chain
 * [✓] No await() usage
 * [✓] No parallel operations on a session
 * [✓] No session/transaction creation in libraries
 */

@Log4j2
@Singleton
public class PlanetService {
    // Stateless cache of the stable "Planet" type classification (detached prepped), keyed by enterpriseId.
    // Resolved via the stateless classificationService.find which returns a detached scalar-prepped row, so
    // it is safe to reuse; only cached on a real hit.
    private static final java.util.Map<UUID, Classification> PLANET_TYPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject
    private IClassificationService<?> classificationService;

    @Inject
    private GeographySecurityCollector securityCollector;

    @Inject
    private GeographyScopeTokenService scopeTokenService;


    public Uni<IGeography<?, ?>> createPlanet(Mutiny.Session session, String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken) {
        // Operate on the caller's session/transaction so writes made earlier in the same
        // install transaction (e.g. the Planet classification) are visible. Opening a nested
        // SessionUtils.withActivityMaster here would start a separate transaction that cannot
        // see those still-uncommitted rows, causing NoResultException on the lookups below.
        var createEnterprise = system.getEnterprise();

        return classificationService.find(session, Planet.toString(), system, identityToken)
                .chain(classification -> {
                    Geography geo = new Geography();
                    return geo.builder(session)
                            .withClassification((Classification) classification)
                            .withName(code)
                            .inDateRange()
                            .inActiveRange()
                            .withEnterprise(createEnterprise)
                            .getCount()
                            .chain(count -> {
                                if (count > 0) {
                                    return findPlanet(session, code, system, identityToken);
                                }

                                geo.setEnterpriseID(createEnterprise);
                                geo.setClassificationID((Classification) classification);
                                geo.setSystemID(system);
                                geo.setOriginalSourceSystemID(system.getId());
                                geo.setName(code);
                                geo.setDescription(description);

                                IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
                                return acService.getActiveFlag(session, createEnterprise, identityToken)
                                        .chain(activeFlag -> {
                                            geo.setActiveFlagID(activeFlag);
                                            return session.persist(geo).replaceWith(Uni.createFrom().item(geo));
                                        })
                                        .chain(persisted -> {
                                            securityCollector.record(session, geo);
                                            // Shadow this root node into the token graph: a scope token nested
                                            // directly under the canonical Everywhere group (parentGeo == null).
                                            Uni<?> setupChain = scopeTokenService.ensureScope(session,
                                                                                              geo,
                                                                                              null,
                                                                                              code,
                                                                                              system,
                                                                                              identityToken
                                            );
                                            if (originalUniqueID != null) {
                                                setupChain = setupChain.chain(() -> geo.addClassification(session,
                                                                                                          GeoNameID.toString(),
                                                                                                          originalUniqueID,
                                                                                                          system,
                                                                                                          identityToken
                                                ));
                                            }
                                            return setupChain.map(result -> (IGeography<?, ?>) geo);
                                        });
                            });
                });
    }

    public Uni<IGeography<?, ?>> findPlanet(Mutiny.Session session, String code, ISystems<?, ?> system, UUID... identityToken) {
        var createEnterprise = system.getEnterprise();

        return classificationService.find(session, Planet.toString(), system, identityToken)
                .chain(classification -> {
                    return new Geography().builder(session)
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

    // ---- Stateless twins ----

    public Uni<IGeography<?, ?>> createPlanet(Mutiny.StatelessSession session, String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken) {
        var enterprise = system.getEnterprise();
        return classificationService.find(session, Planet.toString(), system, identityToken)
                .chain(classification -> new Geography().builder(session)
                        .withClassification((Classification) classification).withName(code).inDateRange().inActiveRange()
                        .withEnterprise(enterprise).getCount()
                        .chain(count -> {
                            if (count > 0) return findPlanet(session, code, system, identityToken);
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
                                    .chain(activeFlag -> {
                                        geo.setActiveFlagID(activeFlag);
                                        return session.insert(geo).replaceWith(geo);
                                    })
                                    .chain(persisted -> {
                                        securityCollector.record(session, geo);
                                        Uni<?> setupChain = scopeTokenService.ensureScope(session,
                                                                                          geo,
                                                                                          null,
                                                                                          code,
                                                                                          system,
                                                                                          identityToken
                                        );
                                        if (originalUniqueID != null)
                                            setupChain = setupChain.chain(() -> geo.addClassification(session,
                                                                                                      GeoNameID.toString(),
                                                                                                      originalUniqueID,
                                                                                                      system,
                                                                                                      identityToken
                                            ));
                                        return setupChain.map(r -> (IGeography<?, ?>) geo);
                                    });
                        }));
    }

    public Uni<IGeography<?, ?>> findPlanet(Mutiny.StatelessSession session, String code, ISystems<?, ?> system, UUID... identityToken) {
        var enterprise = system.getEnterprise();
        UUID enterpriseId = enterprise.getId();
        Classification cachedType = PLANET_TYPE_CACHE.get(enterpriseId);
        Uni<Classification> typeUni = cachedType != null
                ? Uni.createFrom().item(cachedType)
                : classificationService.find(session, Planet.toString(), system, identityToken)
                .map(c -> (Classification) (Object) c)
                .onItem().invoke(c -> {
                    if (c != null && c.getId() != null) PLANET_TYPE_CACHE.put(enterpriseId, c);
                });
        return typeUni
                .chain(classification -> new Geography().builder(session)
                        .withClassification(classification).withName(code).inDateRange().withEnterprise(enterprise).inActiveRange()
                        .get()
                        .onItem().ifNull().failWith(() -> new GeographyException("Unable to find planet"))
                        .map(geo -> (IGeography<?, ?>) geo));
    }
}
