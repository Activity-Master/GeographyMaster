package com.guicedee.activitymaster.geography;


import com.guicedee.activitymaster.fsdm.ClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.annotations.ActivityMasterDB;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography_;
import com.guicedee.activitymaster.fsdm.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedpersistence.db.annotations.Transactional;
import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

import static com.entityassist.enumerations.Operand.*;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

public class ProvinceService
{
	public static final Set<String> ProvinceClassifications = Set.of(Latitude.toString(),
			Longitude.toString(),
			FeatureCodes.toString(),
			FeatureClass.toString(),
			Population.toString(),
			Elevation.toString(),
			DEM.toString());
	
	@CacheResult(cacheName = "GeographyProvinces",
	             skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IGeography<Geography, GeographyQueryBuilder> createProvince(IGeography<Geography, GeographyQueryBuilder> country, @CacheKey String code, String name, String originalUniqueID, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Province, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(code)
		                                .withClassification(classification)
		                                .inActiveRange()
		                                .inDateRange()
		                                .withEnterprise(system)
		                                .getCount() > 0;
		if (exists)
		{
			return findProvince(code, system, identityToken);
		}
		Geography geo = new Geography();
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassification(classification);
		geo.setSystemID(system);
		geo.setOriginalSourceSystemID(system);
		geo.setName(code);
		geo.setDescription(name);
		if (originalUniqueID != null)
		{
			geo.setOriginalSourceSystemUniqueID(originalUniqueID);
		}
		geo.setActiveFlagID(classification.getActiveFlagID());
		geo.persist();
			geo.createDefaultSecurity(system, identityToken);
		country.addChild(geo,NoClassification.toString(),null, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyProvinces")
	public IGeography<Geography, GeographyQueryBuilder> findProvince(@CacheKey String code, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Province, system, identityToken);
		
		return new Geography().builder()
		                      .withClassification(classification)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .withName(code)
		                      .or(Geography_.description,Equals,code)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find province - " + code));
	}
	
	@SuppressWarnings("DuplicatedCode")
	@CacheResult(cacheName = "GeographyProvinces", skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IGeography<Geography, GeographyQueryBuilder> updateProvince(@NotNull @CacheKey String name, String description,
	                           String latitude, String longitude, String featureCodes, String featureClass, Integer population, Integer elevation, Integer dEM,
	                           @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		IGeography<Geography, GeographyQueryBuilder> toUpdate = findProvince(name, system, identityToken);
		if (description != null)
		{
			Geography update = new Geography();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		if (latitude != null)
		{
			toUpdate.addOrUpdateClassification(Latitude, latitude, system, identityToken);
		}
		if (longitude != null)
		{
			toUpdate.addOrUpdateClassification(Longitude, longitude, system, identityToken);
		}
		if (featureClass != null)
		{
			toUpdate.addOrUpdateClassification(FeatureClass, featureClass, system, identityToken);
		}
		if (featureCodes != null)
		{
			toUpdate.addOrUpdateClassification(FeatureCodes, featureCodes, system, identityToken);
		}
		if (population != null)
		{
			toUpdate.addOrUpdateClassification(Population, Integer.toString(population), system, identityToken);
		}
		if (elevation != null)
		{
			toUpdate.addOrUpdateClassification(Elevation, Integer.toString(elevation), system, identityToken);
		}
		if (dEM != null)
		{
			toUpdate.addOrUpdateClassification(DEM, Integer.toString(dEM), system, identityToken);
		}
		
		return toUpdate;
	}
}
