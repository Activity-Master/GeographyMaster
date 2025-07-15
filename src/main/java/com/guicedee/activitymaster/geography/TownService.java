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
import com.google.inject.persist.Transactional;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import jakarta.validation.constraints.NotNull;

import java.util.Set;


import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

public class TownService
{
	public static final Set<String> TownClassifications = Set.copyOf(ProvinceService.ProvinceClassifications);
	
	////@Transactional()
	public IGeography<?,?> createTown(@CacheKey IGeography<Geography, GeographyQueryBuilder> district,
	                                  @CacheKey String name,
	                                  String description,
	                                  String originalUniqueID,
	                                  @CacheKey ISystems<?,?> system,
	                                  @CacheKey java.util.UUID... identityToken)
	{
		ClassificationService classificationService = com.guicedee.client.IGuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(name)
		                                .withClassification(classification)
		                                .inActiveRange()
		                                .inDateRange()
		                                .withEnterprise(system)
		                                .getCount() > 0;
		if (exists)
		{
			return findTown(district,name, system, identityToken);
		}
		
		Geography geo = new Geography();
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassification(classification);
		geo.setSystemID(system);
		geo.setOriginalSourceSystemID(system.getId());
		geo.setName(name);
		geo.setDescription(description);
		if (originalUniqueID != null)
		{
			geo.setOriginalSourceSystemUniqueID(originalUniqueID);
		}
		geo.setActiveFlagID(classification.getActiveFlagID());
		geo.persist();
	
			geo.createDefaultSecurity(system, identityToken);
		
		district.addChild(geo,NoClassification.toString(),null, system, identityToken);
		return geo;
	}
	
	//@CacheResult(cacheName = "GeographyTowns")
	public IGeography<?,?> findTown(@CacheKey IGeography<?,?> district, @CacheKey String name, @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		ClassificationService classificationService = com.guicedee.client.IGuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, system, identityToken);
		
		return new Geography().builder()
		                      .withName(name)
		                      .withClassification(classification)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find town - " + name + " - in district - " + district));
	}
	
	//@CacheResult(cacheName = "GeographyTownNames")
	public IGeography<?,?> findTown( @CacheKey String name, @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		ClassificationService classificationService = com.guicedee.client.IGuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, system, identityToken);
		
		return new Geography().builder()
		                      .withName(name)
		                      .withClassification(classification)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .setReturnFirst(true)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find town - " + name));
	}
	
	@SuppressWarnings("DuplicatedCode")
	//@CacheResult(cacheName = "GeographyTowns", skipGet = true)
	////@Transactional()
	public IGeography<?,?> updateTown(String districtCode, @NotNull @CacheKey String name, String description,
	                                    String latitude, String longitude, String featureCodes, String featureClass, Integer population, Integer elevation, Integer dEM,
	                                    @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		IGeography<?,?> district = com.guicedee.client.IGuiceContext.get(DistrictService.class)
		                                     .findDistrict(districtCode, system, identityToken);
		
		IGeography<?,?> toUpdate = findTown(district, name, system, identityToken);
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
