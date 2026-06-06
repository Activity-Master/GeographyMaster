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
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

@Log4j2
@Singleton
public class TownService
{
	public static final Set<String> TownClassifications = Set.copyOf(ProvinceService.ProvinceClassifications);

	@Inject
	private IClassificationService<?> classificationService;

	@Inject
	private GeographySecurityCollector securityCollector;


	@Inject
	private DistrictService districtService;

	public Uni<IGeography<?, ?>> createTown(Mutiny.Session session, IGeography<?, ?> district,
	                                        String name, String description, String originalUniqueID,
	                                        ISystems<?, ?> system, UUID... identityToken)
	{
		// Operate on the caller's session/transaction (no nested withActivityMaster).
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, Town.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					Geography geo = new Geography();
					return geo.builder(createSession)
						.withName(name)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.getCount()
						.chain(count -> {
							if (count > 0)
							{
								return findTown(createSession, district, name, createSystem, createIdentityToken);
							}

							geo.setEnterpriseID(createEnterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(createSystem);
							geo.setOriginalSourceSystemID(createSystem.getId());
							geo.setName(name);
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
									return setupChain.chain(() -> district.addChild(createSession, geo, NoClassification.toString(), null, createSystem, createIdentityToken)
										.replaceWith((IGeography<?, ?>) geo));
								});
						});
				});
	}

	public Uni<IGeography<?, ?>> findTown(Mutiny.Session session, IGeography<?, ?> district, String name, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, Town.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withName(name)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find town - " + name + " - in district - " + district))
						.map(geo -> (IGeography<?, ?>) geo);
				});
	}

	public Uni<IGeography<?, ?>> findTown(Mutiny.Session session, String name, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, Town.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withName(name)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.setReturnFirst(true)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find town - " + name))
						.map(geo -> (IGeography<?, ?>) geo);
				});
	}

	public Uni<IGeography<?, ?>> updateTown(Mutiny.Session session, String districtCode, @NotNull String name, String description,
	                                        String latitude, String longitude, String featureCodes, String featureClass,
	                                        Integer population, Integer elevation, Integer dEM,
	                                        ISystems<?, ?> system, UUID... identityToken)
	{
		return districtService.findDistrict(session, districtCode, system, identityToken)
			.chain(district -> findTown(session, district, name, system, identityToken))
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
				if (featureClass != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, FeatureClass, featureClass, system, identityToken));
				if (featureCodes != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, FeatureCodes, featureCodes, system, identityToken));
				if (population != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Population, Integer.toString(population), system, identityToken));
				if (elevation != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Elevation, Integer.toString(elevation), system, identityToken));
				if (dEM != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, DEM, Integer.toString(dEM), system, identityToken));
				return chain.replaceWith(toUpdate);
			});
	}
}
