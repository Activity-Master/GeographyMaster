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

import com.google.common.base.Strings;
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
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.text.NumberFormat;
import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

@Log4j2
@Singleton
public class PostalCodeService
{
	public static final Set<String> PostalCodeClassifications = Set.of(Latitude.toString(), Longitude.toString());

	private static final NumberFormat postalCodeFormat = NumberFormat.getInstance();

	static
	{
		postalCodeFormat.setGroupingUsed(false);
		postalCodeFormat.setMaximumFractionDigits(0);
		postalCodeFormat.setMinimumIntegerDigits(4);
	}

	@Inject
	private IClassificationService<?> classificationService;

	@Inject
	private GeographySecurityCollector securityCollector;


	@Inject
	private DistrictService districtService;

	@Inject
	private TownService townService;

	public Uni<IGeography<?, ?>> createPostalCode(Mutiny.Session session, IGeography<?, ?> town, @NotNull String code,
	                                              String description, String originalUniqueID,
	                                              ISystems<?, ?> system, UUID... identityToken)
	{
		String formattedCode = postalCodeFormat.format(Integer.parseInt(code));
		// Operate on the caller's session/transaction (no nested withActivityMaster).
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, PostalCode.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					Geography geo = new Geography();
					return geo.builder(createSession)
						.withName(formattedCode)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.getCount()
						.chain(count -> {
							if (count > 0)
							{
								return findPostalCode(createSession, town, formattedCode, createSystem, createIdentityToken);
							}

							geo.setEnterpriseID(createEnterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(createSystem);
							geo.setOriginalSourceSystemID(createSystem.getId());
							geo.setName(formattedCode);
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
									return setupChain.chain(() -> town.addChild(createSession, geo, NoClassification.toString(), null, createSystem, createIdentityToken)
										.replaceWith((IGeography<?, ?>) geo));
								});
						});
				});
	}

	public Uni<IGeography<?, ?>> createPostalCodeSuburb(Mutiny.Session session, IGeography<?, ?> postalCode, @NotNull String code,
	                                                    @NotNull String description, String originalUniqueID,
	                                                    ISystems<?, ?> system, UUID... identityToken)
	{
		String formattedCode = postalCodeFormat.format(Integer.parseInt(code));
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, PostalCodeSuburb.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					Geography geo = new Geography();
					return geo.builder(createSession)
						.withName(formattedCode)
						.withDescription(description)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.getCount()
						.chain(count -> {
							if (count > 0)
							{
								return findPostalCodeSuburb(createSession, formattedCode, description, createSystem, createIdentityToken);
							}

							geo.setEnterpriseID(createEnterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(createSystem);
							geo.setOriginalSourceSystemID(createSystem.getId());
							geo.setName(formattedCode);
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
									return setupChain.chain(() -> postalCode.addChild(createSession, geo, NoClassification.toString(), null, createSystem, createIdentityToken)
										.replaceWith((IGeography<?, ?>) geo));
								});
						});
				});
	}

	public Uni<IGeography<?, ?>> findPostalCode(Mutiny.Session session, IGeography<?, ?> town, @NotNull String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, PostalCode.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withName(code)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find postal code in town - " + town + " - " + code))
						.map(geo -> (IGeography<?, ?>) geo);
				});
	}

	public Uni<IGeography<?, ?>> findPostalCodeSuburb(Mutiny.Session session, @NotNull String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, PostalCodeSuburb.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withName(code)
						.withDescription(description)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find postal code suburb - " + code))
						.map(geo -> (IGeography<?, ?>) geo);
				});
	}

	public Uni<IGeography<?, ?>> findOrCreatePostalCodeSuburb(Mutiny.Session session, @NotNull String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		return findPostalCodeSuburb(session, code, description, system, identityToken)
			.onFailure().recoverWithUni(error -> {
				return findPostalCode(session, null, code, system, identityToken)
					.onFailure().invoke(e -> log.warn("Unable to find postal code! - {}", code))
					.chain(postalCode -> createPostalCodeSuburb(session, postalCode, code, description, null, system, identityToken));
			});
	}

	public Uni<IGeography<?, ?>> updatePostalCode(Mutiny.Session session, String districtCode, String townCode, @NotNull String code,
	                                              String description, String latitude, String longitude,
	                                              ISystems<?, ?> system, UUID... identityToken)
	{
		Uni<IGeography<?, ?>> findUni;
		if (!Strings.isNullOrEmpty(districtCode))
		{
			findUni = districtService.findDistrict(session, districtCode, system, identityToken)
				.chain(district -> townService.findTown(session, district, townCode, system, identityToken))
				.chain(town -> findPostalCode(session, town, code, system, identityToken));
		}
		else
		{
			findUni = findPostalCode(session, null, code, system, identityToken);
		}

		return findUni.chain(toUpdate -> {
			Uni<?> chain = Uni.createFrom().voidItem();
			if (description != null)
			{
				chain = chain.chain(() -> {
					Geography update = new Geography();
					update.setId(toUpdate.getId());
					update.setDescription(description);
					return session.merge(update).replaceWithVoid();
				});
			}
			if (latitude != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Latitude, latitude, system, identityToken));
			if (longitude != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Longitude, longitude, system, identityToken));
			return chain.replaceWith(toUpdate);
		});
	}

	public Uni<IGeography<?, ?>> updatePostalCodeParent(Mutiny.Session session, @NotNull String code,
	                                                    String description, String latitude, String longitude,
	                                                    ISystems<?, ?> system, UUID... identityToken)
	{
		return findPostalCode(session, null, code, system, identityToken)
			.chain(toUpdate -> {
				Uni<?> chain = Uni.createFrom().voidItem();
				if (description != null)
				{
					chain = chain.chain(() -> {
						Geography update = new Geography();
						update.setId(toUpdate.getId());
						update.setDescription(description);
						return session.merge(update).replaceWithVoid();
					});
				}
				if (latitude != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Latitude, latitude, system, identityToken));
				if (longitude != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Longitude, longitude, system, identityToken));
				return chain.replaceWith(toUpdate);
			});
	}

	// ---- Stateless twins ----

	public Uni<IGeography<?, ?>> createPostalCode(Mutiny.StatelessSession session, IGeography<?, ?> town, @NotNull String code, String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		String formattedCode = postalCodeFormat.format(Integer.parseInt(code));
		var enterprise = system.getEnterprise();
		return classificationService.find(session, PostalCode.toString(), system, identityToken)
				.chain(classification -> new Geography().builder(session)
						.withName(formattedCode).withClassification((Classification) classification).inActiveRange().inDateRange().withEnterprise(enterprise).getCount()
						.chain(count -> {
							if (count > 0) return findPostalCode(session, town, formattedCode, system, identityToken);
							Geography geo = new Geography();
							geo.setId(UUID.randomUUID());
							geo.setEnterpriseID(enterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(system);
							geo.setOriginalSourceSystemID(system.getId());
							geo.setName(formattedCode);
							geo.setDescription(description);
							IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
							return acService.getActiveFlag(session, enterprise, identityToken)
								.chain(activeFlag -> { geo.setActiveFlagID(activeFlag); return session.insert(geo).replaceWith(geo); })
								.chain(persisted -> {
									securityCollector.record(session, geo);
									Uni<?> setupChain = Uni.createFrom().voidItem();
									if (originalUniqueID != null)
										setupChain = setupChain.chain(() -> geo.addClassification(session, GeoNameID.toString(), originalUniqueID, system, identityToken));
									return setupChain.chain(() -> town.addChild(session, geo, NoClassification.toString(), null, system, identityToken).replaceWith((IGeography<?, ?>) geo));
								});
						}));
	}

	public Uni<IGeography<?, ?>> createPostalCodeSuburb(Mutiny.StatelessSession session, IGeography<?, ?> postalCode, @NotNull String code, @NotNull String description, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		String formattedCode = postalCodeFormat.format(Integer.parseInt(code));
		var enterprise = system.getEnterprise();
		return classificationService.find(session, PostalCodeSuburb.toString(), system, identityToken)
				.chain(classification -> new Geography().builder(session)
						.withName(formattedCode).withDescription(description).withClassification((Classification) classification).inActiveRange().inDateRange().withEnterprise(enterprise).getCount()
						.chain(count -> {
							if (count > 0) return findPostalCodeSuburb(session, formattedCode, description, system, identityToken);
							Geography geo = new Geography();
							geo.setId(UUID.randomUUID());
							geo.setEnterpriseID(enterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(system);
							geo.setOriginalSourceSystemID(system.getId());
							geo.setName(formattedCode);
							geo.setDescription(description);
							IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
							return acService.getActiveFlag(session, enterprise, identityToken)
								.chain(activeFlag -> { geo.setActiveFlagID(activeFlag); return session.insert(geo).replaceWith(geo); })
								.chain(persisted -> {
									securityCollector.record(session, geo);
									Uni<?> setupChain = Uni.createFrom().voidItem();
									if (originalUniqueID != null)
										setupChain = setupChain.chain(() -> geo.addClassification(session, GeoNameID.toString(), originalUniqueID, system, identityToken));
									return setupChain.chain(() -> postalCode.addChild(session, geo, NoClassification.toString(), null, system, identityToken).replaceWith((IGeography<?, ?>) geo));
								});
						}));
	}

	public Uni<IGeography<?, ?>> findPostalCode(Mutiny.StatelessSession session, IGeography<?, ?> town, @NotNull String code, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return classificationService.find(session, PostalCode.toString(), system, identityToken)
				.chain(classification -> new Geography().builder(session)
						.withName(code).withClassification((Classification) classification).inActiveRange().inDateRange().withEnterprise(enterprise)
						.get().onItem().ifNull().failWith(() -> new GeographyException("Cannot find postal code in town - " + town + " - " + code))
						.map(geo -> (IGeography<?, ?>) geo));
	}

	public Uni<IGeography<?, ?>> findPostalCodeSuburb(Mutiny.StatelessSession session, @NotNull String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return classificationService.find(session, PostalCodeSuburb.toString(), system, identityToken)
				.chain(classification -> new Geography().builder(session)
						.withName(code).withDescription(description).withClassification((Classification) classification).inActiveRange().inDateRange().withEnterprise(enterprise)
						.get().onItem().ifNull().failWith(() -> new GeographyException("Cannot find postal code suburb - " + code))
						.map(geo -> (IGeography<?, ?>) geo));
	}

	public Uni<IGeography<?, ?>> findOrCreatePostalCodeSuburb(Mutiny.StatelessSession session, @NotNull String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		return findPostalCodeSuburb(session, code, description, system, identityToken)
			.onFailure().recoverWithUni(error -> findPostalCode(session, null, code, system, identityToken)
				.chain(postalCode -> createPostalCodeSuburb(session, postalCode, code, description, null, system, identityToken)));
	}
}
