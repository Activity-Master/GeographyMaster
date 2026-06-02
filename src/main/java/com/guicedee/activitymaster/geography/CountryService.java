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
public class CountryService
{
	public static final Set<String> CountryClassifications = Set.of(CountryISO3166.toString(),
			CountryISO3166_3.toString(),
			CountryISO_Numeric.toString(),
			CountryFips.toString(),
			CountryCapital.toString(),
			CountryAreaInSqKm.toString(),
			CountryTld.toString(),
			CountryPhone.toString(),
			CountryPostalCodeFormat.toString(),
			CountryPostalCodeRegex.toString());

	@Inject
	private IClassificationService<?> classificationService;

	public Uni<IGeography<?, ?>> createCountry(Mutiny.Session session, IGeography<?, ?> continent, @NotNull String iso, @NotNull String description, String originalUniqueID,
	                                           ISystems<?, ?> system, UUID... identityToken)
	{
		return SessionUtils.withActivityMaster(applicationEnterpriseName, system.getName(), tuple -> {
			var createSession = tuple.getItem1();
			var createEnterprise = tuple.getItem2();
			var createSystem = tuple.getItem3();
			var createIdentityToken = tuple.getItem4();

			return classificationService.find(createSession, Country.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					Geography geo = new Geography();
					return geo.builder(createSession)
						.withName(iso)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.getCount()
						.chain(count -> {
							if (count > 0)
							{
								return findCountry(createSession, iso, createSystem, createIdentityToken);
							}

							geo.setEnterpriseID(createEnterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(createSystem);
							geo.setOriginalSourceSystemID(createSystem.getId());
							geo.setName(iso);
							geo.setDescription(description);

							IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
							return acService.getActiveFlag(createSession, createEnterprise, createIdentityToken)
								.chain(activeFlag -> {
									geo.setActiveFlagID(activeFlag);
									return createSession.persist(geo).replaceWith(Uni.createFrom().item(geo));
								})
								.chain(persisted -> {
									Uni<?> setupChain = geo.createDefaultSecurity(createSession, createSystem, createIdentityToken)
										.onFailure().recoverWithItem(() -> null);
									if (originalUniqueID != null)
									{
										setupChain = setupChain.chain(() -> geo.addClassification(createSession, GeoNameID.toString(), originalUniqueID, createSystem, createIdentityToken));
									}
									return setupChain.chain(() -> continent.addChild(createSession, geo, DefaultClassifications.NoClassification.toString(), null, createSystem, createIdentityToken)
										.replaceWith((IGeography<?, ?>) geo));
								});
						});
				});
		});
	}

	public Uni<IGeography<?, ?>> findCountry(Mutiny.Session session, @NotNull String iso, ISystems<?, ?> system, UUID... identityToken)
	{
		return SessionUtils.withActivityMaster(applicationEnterpriseName, system.getName(), tuple -> {
			var createSession = tuple.getItem1();
			var createEnterprise = tuple.getItem2();
			var createSystem = tuple.getItem3();
			var createIdentityToken = tuple.getItem4();

			return classificationService.find(createSession, Country.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withName(iso)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find country - " + iso))
						.map(geo -> (IGeography<?, ?>) geo);
				});
		});
	}

	public Uni<IGeography<?, ?>> updateCountry(Mutiny.Session session, IClassification<?, ?> currency, @NotNull String iso, @NotNull String description,
	                                           String iso3, String isoNumeric, String dialCode, String fips, String capital,
	                                           String areaSqlKM, String postalCodeFormat, String postalCodeRegex, Integer population, String webTld,
	                                           ISystems<?, ?> system, UUID... identityToken)
	{
		return findCountry(session, iso, system, identityToken)
			.chain(geo -> {
				Uni<?> chain = Uni.createFrom().voidItem();
				if (iso != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryISO3166, iso, iso, system, identityToken));
				if (iso3 != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryISO3166_3, iso3, iso3, system, identityToken));
				if (isoNumeric != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryISO_Numeric, isoNumeric, isoNumeric, system, identityToken));
				if (fips != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryFips, fips, fips, system, identityToken));
				if (capital != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryCapital, capital, capital, system, identityToken));
				if (areaSqlKM != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryAreaInSqKm, areaSqlKM, areaSqlKM, system, identityToken));
				if (webTld != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryTld, webTld, webTld, system, identityToken));
				if (population != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, Population, population.toString(), population.toString(), system, identityToken));
				if (dialCode != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryPhone, dialCode, dialCode, system, identityToken));
				if (postalCodeFormat != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryPostalCodeFormat, postalCodeFormat, postalCodeFormat, system, identityToken));
				if (postalCodeRegex != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, CountryPostalCodeRegex, postalCodeRegex, postalCodeRegex, system, identityToken));
				if (currency != null) chain = chain.chain(() -> geo.addOrUpdateClassification(session, currency.toString(), STRING_EMPTY, STRING_EMPTY, system, identityToken));
				return chain.replaceWith(geo);
			});
	}
}
