package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.fsdm.ClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.annotations.ActivityMasterDB;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.fsdm.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedpersistence.db.annotations.Transactional;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import jakarta.validation.constraints.NotNull;

import java.util.*;

import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;

public class DistrictService
{
	public static final Set<String> DistrictClassifications = Set.copyOf(ProvinceService.ProvinceClassifications);
	
	@CacheResult(cacheName = "GeographyDistricts", skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IGeography<?, ?> createDistrict(IGeography<Geography, GeographyQueryBuilder> province, @CacheKey String code, String name, String originalUniqueID, @CacheKey ISystems<?, ?> system, @CacheKey java.util.UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		
		IEnterprise<?, ?> enterprise = system.getEnterprise();
		Classification classification = (Classification) classificationService.find(City, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(code)
		                                .withClassification(classification)
		                                .inActiveRange()
		                                .inDateRange()
		                                .withEnterprise(enterprise)
		                                .getCount() > 0;
		if (exists)
		{
			return findDistrict(code, system, identityToken);
		}
		
		Geography geo = new Geography();
		ISystems<?, ?> geoSystem = GuiceContext.get(com.guicedee.activitymaster.geography.implementations.GeographySystem.class).getSystem(enterprise);
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassification(classification);
		geo.setSystemID(geoSystem);
		geo.setOriginalSourceSystemID(geoSystem);
		geo.setName(code);
		geo.setDescription(name);
		if (originalUniqueID != null)
		{
			geo.setOriginalSourceSystemUniqueID(originalUniqueID);
		}
		geo.setActiveFlagID(classification.getActiveFlagID());
		geo.persist();
		
		geo.createDefaultSecurity(geoSystem, identityToken);
		
		province.addChild(geo, NoClassification.toString(), null, geoSystem, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyDistricts")
	public IGeography<?, ?> findDistrict(@CacheKey String name, @CacheKey ISystems<?, ?> system, @CacheKey java.util.UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		IEnterprise<?, ?> enterprise = system.getEnterprise();
		Classification classification = (Classification) classificationService.find(City, system, identityToken);
		
		return new Geography().builder()
		                      .withName(name)
		                      .withClassification(classification)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(enterprise)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find district / city - " + name));
	}
	
	
	@CacheResult(cacheName = "GeographyDistrictInProvince")
	public IGeography<?, ?> findFirstDistrictInProvince(@CacheKey String provinceCode, @CacheKey ISystems<?, ?> system, @CacheKey java.util.UUID... identityToken)
	{
		ProvinceService ps = get(ProvinceService.class);
		IGeography<?, ?> province = ps.findProvince(provinceCode, system, identityToken);
		var geoLink
				= province.findChildren((String) null, null, system, identityToken)
				          .stream()
				          .findFirst()
				          .orElse(null);
		if (geoLink == null)
		{
			return null;
		}
		return geoLink.getSecondary();
	}
	
	@CacheResult(cacheName = "GeographyDistricts")
	public List<Geography> findAllDistricts(@CacheKey ISystems<?, ?> system, @CacheKey java.util.UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		IEnterprise<?, ?> enterprise = system.getEnterprise();
		Classification classification = (Classification) classificationService.find(City, system, identityToken);
		return new Geography().builder()
		                      .withClassification(classification)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(enterprise)
		                      .getAll();
	}
	
	@CacheResult(cacheName = "GeographyDistricts", skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IGeography<?, ?> updateDistrict(@NotNull @CacheKey String name, String description,
	                                       String latitude, String longitude, String featureCodes, String featureClass, Integer population, Integer elevation, Integer dEM,
	                                       @CacheKey ISystems<?, ?> system, @CacheKey java.util.UUID... identityToken)
	{
		IEnterprise<?, ?> enterprise = system.getEnterprise();
		IGeography<?, ?> toUpdate = findDistrict(name, system, identityToken);
		if (description != null)
		{
			Geography update = new Geography();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		if (latitude != null)
		{
			toUpdate.addOrUpdateClassification(Latitude, latitude, latitude, system, identityToken);
		}
		if (longitude != null)
		{
			toUpdate.addOrUpdateClassification(Longitude, longitude, longitude, system, identityToken);
		}
		if (featureClass != null)
		{
			toUpdate.addOrUpdateClassification(FeatureClass, featureClass, featureClass, system, identityToken);
		}
		if (featureCodes != null)
		{
			toUpdate.addOrUpdateClassification(FeatureCodes, featureCodes, featureCodes, system, identityToken);
		}
		if (population != null)
		{
			toUpdate.addOrUpdateClassification(Population, Integer.toString(population), Integer.toString(population), system, identityToken);
		}
		if (elevation != null)
		{
			toUpdate.addOrUpdateClassification(Elevation, Integer.toString(elevation), Integer.toString(elevation), system, identityToken);
		}
		if (dEM != null)
		{
			toUpdate.addOrUpdateClassification(DEM, Integer.toString(dEM), Integer.toString(dEM), system, identityToken);
		}
		
		return toUpdate;
	}
}
