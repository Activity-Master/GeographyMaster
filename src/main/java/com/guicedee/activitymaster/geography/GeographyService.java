package com.guicedee.activitymaster.geography;

import com.google.inject.Singleton;
import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.implementations.ClassificationService;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.system.ISystemsService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.Optional;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;

@Singleton
public class GeographyService<J extends GeographyService<J>>
		implements IGeographyService<J>
{
	@CacheResult(cacheName = "GeographyPlanets",
			skipGet = true)
	public IGeography<?> createPlanet(ISystems<?> originatingSystem, @CacheKey @NotNull String value, @Null String originalUniqueID, UUID... identifyingToken)
	{
		Geography geo = new Geography();
		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(originatingSystem.getEnterpriseID());
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		ISystems<?> geoSystem = GeographySystem.getSystemsMap()
		                                       .get(activityMasterSystem.getEnterpriseID());

		Classification classification = (Classification) classificationService.find(Planet, originatingSystem.getEnterprise(), identifyingToken);

		Optional<Geography> geographyExists = geo.builder()
		                                         .withClassification(classification, null)
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .findByName(value)
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		if (geographyExists.isEmpty())
		{
			geo.setEnterpriseID(classification.getEnterpriseID());
			geo.setClassification(classification);
			geo.setSystemID((Systems) geoSystem);
			geo.setOriginalSourceSystemID((Systems) geoSystem);
			geo.setName(value);
			geo.setDescription("The Planet " + value);
			if (originalUniqueID != null)
			{
				geo.setOriginalSourceSystemUniqueID(originalUniqueID);
			}
			geo.setActiveFlagID(classification.getActiveFlagID());
			geo.persist();
			if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
			{
				geo.createDefaultSecurity(geoSystem, identifyingToken);
			}
		}
		else
		{
			geo = geographyExists.get();
		}
		return geo;
	}

	@Override
	@CacheResult(cacheName = "GeographyPlanets")
	public IGeography<?> findPlanet(@CacheKey String name, ISystems<?> originatingSystem, UUID... identifyingToken)
	{
		Geography geo = new Geography();
		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(originatingSystem.getEnterpriseID());
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Planet, originatingSystem.getEnterprise(), identifyingToken);

		Optional<Geography> geographyExists = geo.builder()
		                                         .withClassification(classification, null)
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .findByName(name)
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		return geographyExists.orElseThrow(() -> new GeographyException("Planet does not exist - " + name));
	}

	@CacheResult(cacheName = "GeographyContinents")
	public IGeography<?> createContinent(
			@CacheKey String planet, @CacheKey GeographyContinent continent, ISystems<?> originatingSystem, @Null String originalUniqueID, UUID... identifyingToken)
	{
		Geography geo = new Geography();
		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(originatingSystem.getEnterpriseID());
		ISystems<?> geoSystem = GeographySystem.getSystemsMap()
		                                       .get(activityMasterSystem.getEnterpriseID());

		Geography planetGeo = (Geography) findPlanet(planet, originatingSystem, identifyingToken);

		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Continent, originatingSystem.getEnterprise(), identifyingToken);

		Optional<Geography> geographyExists = geo.builder()
		                                         .withClassification(classification, null)
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .withParent(planetGeo, null)
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		if (geographyExists.isEmpty())
		{
			geo.setEnterpriseID(classification.getEnterpriseID());
			geo.setClassification(classification);
			geo.setSystemID((Systems) geoSystem);
			geo.setOriginalSourceSystemID((Systems) geoSystem);
			geo.setName(continent.getContinentName());
			geo.setDescription("The continent " + continent.getContinentName());
			if (originalUniqueID != null)
			{
				geo.setOriginalSourceSystemUniqueID(originalUniqueID);
			}
			geo.setActiveFlagID(classification.getActiveFlagID());
			geo.persist();
			if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
			{
				geo.createDefaultSecurity(geoSystem, identifyingToken);
			}
			geo.add(ContinentCode, continent.getContinentCode(), originatingSystem, identifyingToken);
			planetGeo.addChild(geo, originatingSystem.getEnterprise(), identifyingToken);
		}
		else
		{
			geo = geographyExists.get();
		}
		return geo;
	}

	@Override
	@CacheResult(cacheName = "GeographyContinents")
	public GeographyContinent findContinent(GeographyContinent continent, ISystems<?> originatingSystem, UUID... identifyingToken)
	{
		Geography geo = new Geography();
		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(originatingSystem.getEnterpriseID());
		ISystems<?> geoSystem = GeographySystem.getSystemsMap()
		                                       .get(activityMasterSystem.getEnterpriseID());

		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Continent, originatingSystem.getEnterprise(), identifyingToken);
		Classification classificationCode = (Classification) classificationService.find(ContinentCode, originatingSystem.getEnterprise(), identifyingToken);

		GeographyQueryBuilder queryBuilder = geo.builder()
		                                        .withClassification(classification, null)
		                                        .withEnterprise(originatingSystem.getEnterpriseID())
		                                        .findByName(continent.getContinentName())
		                                        .inDateRange()
		                                        .withEnterprise(activityMasterSystem.getEnterprise());

		if (continent.getContinentCode() != null)
		{
			queryBuilder.withClassification(classificationCode, continent.getContinentCode());
		}

		Optional<Geography> geographyExists = geo.builder()
		                                         .withClassification(classification, null)
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .findByName(continent.getContinentName())
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		Geography geoOut = geographyExists.orElseThrow(() -> new GeographyException("Continent does not exist - " + continent));

		return new GeographyContinent().setContinentName(geoOut.getName())
		                               .setContinentCode(geo.find(ContinentCode, geoSystem, identifyingToken)
		                                                    .orElseThrow(() -> new GeographyException(
				                                                    "Continent does not have a continent code somehow?!? - " + continent))
		                                                    .getValue());

	}

}
