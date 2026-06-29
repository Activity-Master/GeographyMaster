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
import com.guicedee.activitymaster.geography.implementations.GeographySystem;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

@Log4j2
@Singleton
public class DistrictService
{
	public static final Set<String> DistrictClassifications = Set.copyOf(ProvinceService.ProvinceClassifications);

	@Inject
	private IClassificationService<?> classificationService;

	@Inject
	private GeographySecurityCollector securityCollector;


	@Inject
	private ProvinceService provinceService;

	public Uni<IGeography<?, ?>> createDistrict(Mutiny.Session session, IGeography<?, ?> province, String code, String name, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		// Operate on the caller's session/transaction (no nested withActivityMaster).
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, City.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					Geography geo = new Geography();
					return geo.builder(createSession)
						.withName(code)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.getCount()
						.chain(count -> {
							if (count > 0)
							{
								return findDistrict(createSession, code, createSystem, createIdentityToken);
							}

							geo.setEnterpriseID(createEnterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(createSystem);
							geo.setOriginalSourceSystemID(createSystem.getId());
							geo.setName(code);
							geo.setDescription(name);

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
									return setupChain.chain(() -> province.addChild(createSession, geo, NoClassification.toString(), null, createSystem, createIdentityToken)
										.replaceWith((IGeography<?, ?>) geo));
								});
						});
				});
	}

	public Uni<IGeography<?, ?>> findDistrict(Mutiny.Session session, String name, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, City.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withName(name)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find district / city - " + name))
						.map(geo -> (IGeography<?, ?>) geo);
				});
	}

	public Uni<IGeography<?, ?>> findFirstDistrictInProvince(Mutiny.Session session, String provinceCode, ISystems<?, ?> system, UUID... identityToken)
	{
		return provinceService.findProvince(session, provinceCode, system, identityToken)
			.chain(province -> {
				return province.findChildren(session, (String) null, null, system, identityToken)
					.map(children -> {
						if (children == null || children.isEmpty())
						{
							return null;
						}
						return (IGeography<?, ?>) children.stream().findFirst().orElse(null).getSecondary();
					});
			});
	}

	public Uni<List<Geography>> findAllDistricts(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{
		var createSession = session;
		var createEnterprise = system.getEnterprise();
		var createSystem = system;
		var createIdentityToken = identityToken;

		return classificationService.find(createSession, City.toString(), createSystem, createIdentityToken)
				.chain(classification -> {
					return new Geography().builder(createSession)
						.withClassification((Classification) classification)
						.inActiveRange()
						.inDateRange()
						.withEnterprise(createEnterprise)
						.getAll();
				});
	}

	public Uni<IGeography<?, ?>> updateDistrict(Mutiny.Session session, @NotNull String name, String description,
	                                            String latitude, String longitude, String featureCodes, String featureClass,
	                                            Integer population, Integer elevation, Integer dEM,
	                                            ISystems<?, ?> system, UUID... identityToken)
	{
		return findDistrict(session, name, system, identityToken)
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
				if (latitude != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Latitude, latitude, latitude, system, identityToken));
				if (longitude != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Longitude, longitude, longitude, system, identityToken));
				if (featureClass != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, FeatureClass, featureClass, featureClass, system, identityToken));
				if (featureCodes != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, FeatureCodes, featureCodes, featureCodes, system, identityToken));
				if (population != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Population, Integer.toString(population), Integer.toString(population), system, identityToken));
				if (elevation != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, Elevation, Integer.toString(elevation), Integer.toString(elevation), system, identityToken));
				if (dEM != null) chain = chain.chain(() -> toUpdate.addOrUpdateClassification(session, DEM, Integer.toString(dEM), Integer.toString(dEM), system, identityToken));
				return chain.replaceWith(toUpdate);
			});
	}

	// ---- Stateless twins ----

	public Uni<IGeography<?, ?>> createDistrict(Mutiny.StatelessSession session, IGeography<?, ?> province, String code, String name, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return classificationService.find(session, City.toString(), system, identityToken)
				.chain(classification -> new Geography().builder(session)
						.withName(code).withClassification((Classification) classification).inActiveRange().inDateRange().withEnterprise(enterprise).getCount()
						.chain(count -> {
							if (count > 0) return findDistrict(session, code, system, identityToken);
							Geography geo = new Geography();
							geo.setId(UUID.randomUUID());
							geo.setEnterpriseID(enterprise);
							geo.setClassificationID((Classification) classification);
							geo.setSystemID(system);
							geo.setOriginalSourceSystemID(system.getId());
							geo.setName(code);
							geo.setDescription(name);
							IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
							return acService.getActiveFlag(session, enterprise, identityToken)
								.chain(activeFlag -> { geo.setActiveFlagID(activeFlag); return session.insert(geo).replaceWith(geo); })
								.chain(persisted -> {
									securityCollector.record(session, geo);
									Uni<?> setupChain = Uni.createFrom().voidItem();
									if (originalUniqueID != null)
										setupChain = setupChain.chain(() -> geo.addClassification(session, GeoNameID.toString(), originalUniqueID, system, identityToken));
									return setupChain.chain(() -> province.addChild(session, geo, NoClassification.toString(), null, system, identityToken).replaceWith((IGeography<?, ?>) geo));
								});
						}));
	}

	public Uni<IGeography<?, ?>> findDistrict(Mutiny.StatelessSession session, String name, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return classificationService.find(session, City.toString(), system, identityToken)
				.chain(classification -> new Geography().builder(session)
						.withName(name).withClassification((Classification) classification).inActiveRange().inDateRange().withEnterprise(enterprise)
						.get()
						.onItem().ifNull().failWith(() -> new GeographyException("Cannot find district / city - " + name))
						.map(geo -> (IGeography<?, ?>) geo));
	}

	public Uni<IGeography<?, ?>> findFirstDistrictInProvince(Mutiny.StatelessSession session, String provinceCode, ISystems<?, ?> system, UUID... identityToken)
	{
		return provinceService.findProvince(session, provinceCode, system, identityToken)
			.chain(province -> province.findChildren(session, (String) null, null, system, identityToken)
				.map(children -> (children == null || children.isEmpty()) ? null
						: (IGeography<?, ?>) children.stream().findFirst().orElse(null).getSecondary()));
	}

	public Uni<List<Geography>> findAllDistricts(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken)
	{
		var enterprise = system.getEnterprise();
		return classificationService.find(session, City.toString(), system, identityToken)
				.chain(classification -> new Geography().builder(session)
						.withClassification((Classification) classification).inActiveRange().inDateRange().withEnterprise(enterprise).getAll());
	}
}
