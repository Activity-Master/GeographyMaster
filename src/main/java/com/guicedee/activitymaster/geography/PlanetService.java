package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.fsdm.ClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.annotations.ActivityMasterDB;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.fsdm.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedpersistence.db.annotations.Transactional;
import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;

import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

public class PlanetService
{
	@CacheResult(cacheName = "GeographyPlanets",
	             skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IGeography<Geography, GeographyQueryBuilder> createPlanet(@CacheKey String code, String description, String originalUniqueID, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Planet, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withClassification(classification)
		                                .withName(code)
		                                .inDateRange()
		                                .inActiveRange()
		                                .withEnterprise(system)
		                                .getCount() > 0;
		
		if (exists)
		{
			return findPlanet(code, system, identityToken);
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
		
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyPlanets")
	public IGeography<Geography, GeographyQueryBuilder> findPlanet(@CacheKey String code, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Planet, system, identityToken);
		
		return new Geography().builder()
		                      .withClassification(classification)
		                      .withName(code)
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .inActiveRange()
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Unable to find planet"));
	}
}
