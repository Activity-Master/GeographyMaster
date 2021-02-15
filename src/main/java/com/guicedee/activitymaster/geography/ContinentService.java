package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.ClassificationService;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;

import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;

import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.Continent;
import static com.guicedee.guicedinjection.GuiceContext.get;

public class ContinentService
{
	@CacheResult(cacheName = "GeographyContinents",skipGet = true)
	public IGeography<?> createContinent(IGeography<?> planet, @CacheKey String code, String description, String originalUniqueID,@CacheKey ISystems<?> system,@CacheKey  UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Continent, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withClassification(classification)
		                                .withName(code)
		                                .inDateRange()
		                                .inActiveRange(system, identityToken)
		                                .withEnterprise(system)
		                                .getCount() > 0;
		if(exists)
		{
			return findContinent(code, system, identityToken);
		}
		Geography geo = new Geography();
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassification(classification);
		geo.setSystemID((Systems) system);
		geo.setOriginalSourceSystemID((Systems) system);
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
			geo.createDefaultSecurity(system, identityToken);
		}
		
		planet.addChild(geo, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyContinents",skipGet = true)
	public IGeography<?> findContinent(@CacheKey String code,@CacheKey  ISystems<?> system,@CacheKey  UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Continent, system, identityToken);
		
		return new Geography().builder()
		                      .withClassification(classification)
		                      .withName(code)
		                      .inDateRange()
		                      .inActiveRange(system, identityToken)
		                      .withEnterprise(system)
				.get().orElseThrow(()->new GeographyException("Cannot find continent"));
	}
}
