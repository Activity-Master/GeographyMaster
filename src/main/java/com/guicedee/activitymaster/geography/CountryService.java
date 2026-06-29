package com.guicedee.activitymaster.geography;

/**
 * Reactivity Migration Checklist:
 * <p>
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
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.modules.services.jsonrepresentation.json.StaticStrings.*;

@Log4j2
@Singleton
public class CountryService {
    public static final Set<String> CountryClassifications = Set.of(CountryISO3166.toString(),
                                                                    CountryISO3166_3.toString(),
                                                                    CountryISO_Numeric.toString(),
                                                                    CountryFips.toString(),
                                                                    CountryCapital.toString(),
                                                                    CountryAreaInSqKm.toString(),
                                                                    CountryTld.toString(),
                                                                    CountryPhone.toString(),
                                                                    CountryPostalCodeFormat.toString(),
                                                                    CountryPostalCodeRegex.toString()
    );

    // Stateless cache of the stable "Country" type classification (detached prepped), keyed by enterpriseId.
    // Resolved via the stateless classificationService.find (detached scalar-prepped), safe to reuse; cached on hit.
    private static final java.util.Map<UUID, Classification> COUNTRY_TYPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject
    private IClassificationService<?> classificationService;

    @Inject
    private GeographySecurityCollector securityCollector;

    @Inject
    private GeographyScopeTokenService scopeTokenService;

    public Uni<IGeography<?, ?>> createCountry(Mutiny.Session session, IGeography<?, ?> continent, @NotNull String iso, @NotNull String description, String originalUniqueID,
                                               ISystems<?, ?> system, UUID... identityToken
    ) {
        // Use the caller's session/transaction so prior writes in the same transaction remain visible.
        var createEnterprise = system.getEnterprise();

        return classificationService.find(session, Country.toString(), system, identityToken)
                .chain(classification -> {
                    Geography geo = new Geography();
                    return geo.builder(session)
                            .withName(iso)
                            .withClassification(classification)
                            .inActiveRange()
                            .inDateRange()
                            .withEnterprise(createEnterprise)
                            .getCount()
                            .chain(count -> {
                                if (count > 0) {
                                    return findCountry(session, iso, system, identityToken);
                                }

                                geo.setEnterpriseID(createEnterprise);
                                geo.setClassificationID((Classification) classification);
                                geo.setSystemID(system);
                                geo.setOriginalSourceSystemID(system.getId());
                                geo.setName(iso);
                                geo.setDescription(description);

                                IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
                                return acService.getActiveFlag(session, createEnterprise, identityToken)
                                        .chain(activeFlag -> {
                                            geo.setActiveFlagID(activeFlag);
                                            return session.persist(geo).replaceWith(Uni.createFrom().item(geo));
                                        })
                                        .chain(persisted -> {
                                            // Record for batched default-security at the end of the load phase
                                            // instead of paying the per-row security cost here.
                                            securityCollector.record(session, geo);
                                            // Shadow this node into the token graph under its continent's scope token.
                                            Uni<?> setupChain = scopeTokenService.ensureScope(session,
                                                                                              geo,
                                                                                              continent,
                                                                                              description,
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
                                            return setupChain.chain(() -> continent.addChild(session,
                                                                                             geo,
                                                                                             DefaultClassifications.NoClassification.toString(),
                                                                                             null,
                                                                                             system,
                                                                                             identityToken
                                                    )
                                                    .replaceWith((IGeography<?, ?>) geo));
                                        });
                            });
                });
    }

    public Uni<IGeography<?, ?>> findCountry(Mutiny.Session session, @NotNull String iso, ISystems<?, ?> system, UUID... identityToken) {
        var createEnterprise = system.getEnterprise();

        return classificationService.find(session, Country.toString(), system, identityToken)
                .chain(classification -> {
                    return new Geography().builder(session)
                            .withName(iso)
                            .withClassification((Classification) classification)
                            .inActiveRange()
                            .inDateRange()
                            .withEnterprise(createEnterprise)
                            // Read-only lookup of immutable country reference data: skips dirty-check
                            // snapshots and pre-query auto-flush, and (with 2LC) is served from the cache.
                            .setReadOnly(true)
                            .get()
                            .onItem().ifNull().failWith(() -> new GeographyException("Cannot find country - " + iso))
                            .map(geo -> (IGeography<?, ?>) geo);
                });
    }

    public Uni<IGeography<?, ?>> updateCountry(Mutiny.Session session, IClassification<?, ?> currency, @NotNull String iso, @NotNull String description,
                                               String iso3, String isoNumeric, String dialCode, String fips, String capital,
                                               String areaSqlKM, String postalCodeFormat, String postalCodeRegex, Integer population, String webTld,
                                               ISystems<?, ?> system, UUID... identityToken
    ) {
        return findCountry(session, iso, system, identityToken)
                .chain(geo -> {
                    Uni<?> chain = Uni.createFrom().voidItem();
                    if (iso != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                             CountryISO3166,
                                                                                             iso,
                                                                                             iso,
                                                                                             system,
                                                                                             identityToken
                    ));
                    if (iso3 != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                              CountryISO3166_3,
                                                                                              iso3,
                                                                                              iso3,
                                                                                              system,
                                                                                              identityToken
                    ));
                    if (isoNumeric != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                    CountryISO_Numeric,
                                                                                                    isoNumeric,
                                                                                                    isoNumeric,
                                                                                                    system,
                                                                                                    identityToken
                    ));
                    if (fips != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                              CountryFips,
                                                                                              fips,
                                                                                              fips,
                                                                                              system,
                                                                                              identityToken
                    ));
                    if (capital != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                 CountryCapital,
                                                                                                 capital,
                                                                                                 capital,
                                                                                                 system,
                                                                                                 identityToken
                    ));
                    if (areaSqlKM != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                   CountryAreaInSqKm,
                                                                                                   areaSqlKM,
                                                                                                   areaSqlKM,
                                                                                                   system,
                                                                                                   identityToken
                    ));
                    if (webTld != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                CountryTld,
                                                                                                webTld,
                                                                                                webTld,
                                                                                                system,
                                                                                                identityToken
                    ));
                    if (population != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                    Population,
                                                                                                    population.toString(),
                                                                                                    population.toString(),
                                                                                                    system,
                                                                                                    identityToken
                    ));
                    if (dialCode != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                  CountryPhone,
                                                                                                  dialCode,
                                                                                                  dialCode,
                                                                                                  system,
                                                                                                  identityToken
                    ));
                    if (postalCodeFormat != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                          CountryPostalCodeFormat,
                                                                                                          postalCodeFormat,
                                                                                                          postalCodeFormat,
                                                                                                          system,
                                                                                                          identityToken
                    ));
                    if (postalCodeRegex != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                         CountryPostalCodeRegex,
                                                                                                         postalCodeRegex,
                                                                                                         postalCodeRegex,
                                                                                                         system,
                                                                                                         identityToken
                    ));
                    // Currency classifications (e.g. "EUR") live under the Currency concept (ClassificationXClassification),
                    // not the default NoClassificationDataConceptName. Thread the concept so the lookup resolves the
                    // correct classification instead of failing/colliding on a duplicate name in another concept.
                    if (currency != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                  currency.toString(),
                                                                                                  Currency.concept(),
                                                                                                  STRING_EMPTY,
                                                                                                  STRING_EMPTY,
                                                                                                  system,
                                                                                                  identityToken
                    ));
                    return chain.replaceWith(geo);
                });
    }

    // ---- Stateless twins ----

    public Uni<IGeography<?, ?>> createCountry(Mutiny.StatelessSession session, IGeography<?, ?> continent, @NotNull String iso, @NotNull String description, String originalUniqueID,
                                               ISystems<?, ?> system, UUID... identityToken
    ) {
        var enterprise = system.getEnterprise();
        return classificationService.find(session, Country.toString(), system, identityToken)
                .chain(classification -> new Geography().builder(session)
                        .withName(iso).withClassification((Classification) classification).inActiveRange().inDateRange()
                        .withEnterprise(enterprise).getCount()
                        .chain(count -> {
                            if (count > 0) return findCountry(session, iso, system, identityToken);
                            Geography geo = new Geography();
                            geo.setId(UUID.randomUUID());
                            geo.setEnterpriseID(enterprise);
                            geo.setClassificationID((Classification) classification);
                            geo.setSystemID(system);
                            geo.setOriginalSourceSystemID(system.getId());
                            geo.setName(iso);
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
                                                                                          continent,
                                                                                          description,
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
                                        return setupChain.chain(() -> continent.addChild(session,
                                                                                         geo,
                                                                                         DefaultClassifications.NoClassification.toString(),
                                                                                         null,
                                                                                         system,
                                                                                         identityToken
                                        ).replaceWith((IGeography<?, ?>) geo));
                                    });
                        }));
    }

    public Uni<IGeography<?, ?>> findCountry(Mutiny.StatelessSession session, @NotNull String iso, ISystems<?, ?> system, UUID... identityToken) {
        var enterprise = system.getEnterprise();
        UUID enterpriseId = enterprise.getId();
        Classification cachedType = COUNTRY_TYPE_CACHE.get(enterpriseId);
        Uni<Classification> typeUni = cachedType != null
                ? Uni.createFrom().item(cachedType)
                : classificationService.find(session, Country.toString(), system, identityToken)
                .map(c -> (Classification) (Object) c)
                .onItem().invoke(c -> {
                    if (c != null && c.getId() != null) COUNTRY_TYPE_CACHE.put(enterpriseId, c);
                });
        return typeUni
                .chain(classification -> new Geography().builder(session)
                        .withName(iso).withClassification(classification).inActiveRange().inDateRange().withEnterprise(
                                enterprise).setReadOnly(true)
                        .get()
                        .onItem().ifNull().failWith(() -> new GeographyException("Cannot find country - " + iso))
                        .map(geo -> (IGeography<?, ?>) geo));
    }

    public Uni<IGeography<?, ?>> updateCountry(Mutiny.StatelessSession session, IClassification<?, ?> currency, @NotNull String iso, @NotNull String description,
                                               String iso3, String isoNumeric, String dialCode, String fips, String capital,
                                               String areaSqlKM, String postalCodeFormat, String postalCodeRegex, Integer population, String webTld,
                                               ISystems<?, ?> system, UUID... identityToken
    ) {
        return findCountry(session, iso, system, identityToken)
                .chain(geo -> {
                    Uni<?> chain = Uni.createFrom().voidItem();
                    if (iso != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                             CountryISO3166,
                                                                                             iso,
                                                                                             iso,
                                                                                             system,
                                                                                             identityToken
                    ));
                    if (iso3 != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                              CountryISO3166_3,
                                                                                              iso3,
                                                                                              iso3,
                                                                                              system,
                                                                                              identityToken
                    ));
                    if (isoNumeric != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                    CountryISO_Numeric,
                                                                                                    isoNumeric,
                                                                                                    isoNumeric,
                                                                                                    system,
                                                                                                    identityToken
                    ));
                    if (fips != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                              CountryFips,
                                                                                              fips,
                                                                                              fips,
                                                                                              system,
                                                                                              identityToken
                    ));
                    if (capital != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                 CountryCapital,
                                                                                                 capital,
                                                                                                 capital,
                                                                                                 system,
                                                                                                 identityToken
                    ));
                    if (areaSqlKM != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                   CountryAreaInSqKm,
                                                                                                   areaSqlKM,
                                                                                                   areaSqlKM,
                                                                                                   system,
                                                                                                   identityToken
                    ));
                    if (webTld != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                CountryTld,
                                                                                                webTld,
                                                                                                webTld,
                                                                                                system,
                                                                                                identityToken
                    ));
                    if (population != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                    Population,
                                                                                                    population.toString(),
                                                                                                    population.toString(),
                                                                                                    system,
                                                                                                    identityToken
                    ));
                    if (dialCode != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                  CountryPhone,
                                                                                                  dialCode,
                                                                                                  dialCode,
                                                                                                  system,
                                                                                                  identityToken
                    ));
                    if (postalCodeFormat != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                          CountryPostalCodeFormat,
                                                                                                          postalCodeFormat,
                                                                                                          postalCodeFormat,
                                                                                                          system,
                                                                                                          identityToken
                    ));
                    if (postalCodeRegex != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                         CountryPostalCodeRegex,
                                                                                                         postalCodeRegex,
                                                                                                         postalCodeRegex,
                                                                                                         system,
                                                                                                         identityToken
                    ));
                    if (currency != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session,
                                                                                                  currency.toString(),
                                                                                                  STRING_EMPTY,
                                                                                                  STRING_EMPTY,
                                                                                                  system,
                                                                                                  identityToken
                    ));
                    return chain.replaceWith(geo);
                });
    }
}
