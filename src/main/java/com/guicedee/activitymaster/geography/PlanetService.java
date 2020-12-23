package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.implementations.ClassificationService;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;

import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.Planet;
import static com.guicedee.guicedinjection.GuiceContext.get;

@Singleton
public class PlanetService
{
	@CacheResult(cacheName = "GeographyPlanets",
	             skipGet = true)
	public IGeography<?> createPlanet(@CacheKey String code, String description, String originalUniqueID, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Planet, enterprise, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withClassification(classification)
		                                .withName(code)
		                                .inDateRange()
		                                .inActiveRange(enterprise, identityToken)
		                                .withEnterprise(enterprise)
		                                .getCount() > 0;
		
		if (exists)
		{
			return findPlanet(code, enterprise, identityToken);
		}
		Geography geo = new Geography();
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassification(classification);
		geo.setSystemID((Systems) geoSystem);
		geo.setOriginalSourceSystemID((Systems) geoSystem);
		geo.setName(code);
		geo.setDescription(description);
		if (originalUniqueID != null)
		{
			geo.setOriginalSourceSystemUniqueID(originalUniqueID);
		}
		geo.setActiveFlagID(classification.getActiveFlagID());
		geo.persist();
		if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
		{
			geo.createDefaultSecurity(geoSystem, identityToken);
		}
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyPlanets")
	public IGeography<?> findPlanet(@CacheKey String code, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Planet, enterprise, identityToken);
		
		return new Geography().builder()
		                      .withClassification(classification)
		                      .withName(code)
		                      .inDateRange()
		                      .withEnterprise(enterprise)
		                      .inActiveRange(enterprise, identityToken)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Unable to find planet"));
	}
}
