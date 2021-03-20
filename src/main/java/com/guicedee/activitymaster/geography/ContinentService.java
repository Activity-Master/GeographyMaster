package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.core.ClassificationService;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;

import java.util.UUID;

import static com.guicedee.activitymaster.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

public class ContinentService
{
	@CacheResult(cacheName = "GeographyContinents",skipGet = true)
	public IGeography<Geography, GeographyQueryBuilder> createContinent(IGeography<Geography, GeographyQueryBuilder> planet, @CacheKey String code, String description, String originalUniqueID, @CacheKey ISystems<?,?> system, @CacheKey  UUID... identityToken)
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
		geo.setSystemID(system);
		geo.setOriginalSourceSystemID(system);
		geo.setName(code);
		geo.setDescription(description);
		if (originalUniqueID != null)
		{
			geo.setOriginalSourceSystemUniqueID(originalUniqueID);
		}
		geo.setActiveFlagID(classification.getActiveFlagID());
		geo.persist();
	
			geo.createDefaultSecurity(system, identityToken);
		
		
		planet.addChild(geo,NoClassification.toString(),null, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyContinents",skipGet = true)
	public IGeography<Geography, GeographyQueryBuilder> findContinent(@CacheKey String code,@CacheKey  ISystems<?,?> system,@CacheKey  UUID... identityToken)
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
