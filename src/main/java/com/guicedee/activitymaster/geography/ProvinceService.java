package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.async.AsyncPersist;
import com.guicedee.activitymaster.core.async.AsyncUpdate;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.geography.Geography_;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.implementations.ClassificationService;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationValue;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;

import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

import static com.entityassist.enumerations.Operand.Equals;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.DEM;
import static com.guicedee.guicedinjection.GuiceContext.get;

public class ProvinceService
{
	public static final Set<IClassificationValue<?>> ProvinceClassifications = Set.of(Latitude, Longitude, FeatureCodes, FeatureClass, Population, Elevation, DEM);
	
	@CacheResult(cacheName = "GeographyProvinces",
	             skipGet = true)
	public IGeography<?> createProvince(IGeography<?> country, @CacheKey String code, String name, String originalUniqueID, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Province, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(code)
		                                .withClassification(classification)
		                                .inActiveRange(system, identityToken)
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
		geo.setSystemID((Systems) system);
		geo.setOriginalSourceSystemID((Systems) system);
		geo.setName(code);
		geo.setDescription(name);
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
		country.addChild(geo, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyProvinces")
	public IGeography<?> findProvince(@CacheKey String code, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Province, system, identityToken);
		
		return new Geography().builder()
		                      .withClassification(classification)
		                      .inActiveRange(system, identityToken)
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .withName(code)
		                      .or(Geography_.description,Equals,code)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find province - " + code));
	}
	
	@SuppressWarnings("DuplicatedCode")
	@CacheResult(cacheName = "GeographyProvinces", skipGet = true)
	public IGeography<?> updateProvince(@NotNull @CacheKey String name, String description,
	                           String latitude, String longitude, String featureCodes, String featureClass, Integer population, Integer elevation, Integer dEM,
	                           @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		IGeography<?> toUpdate = findProvince(name, system, identityToken);
		if (description != null)
		{
			Geography update = new Geography();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		if (latitude != null)
		{
			toUpdate.addOrUpdate(Latitude, latitude, system, identityToken);
		}
		if (longitude != null)
		{
			toUpdate.addOrUpdate(Longitude, longitude, system, identityToken);
		}
		if (featureClass != null)
		{
			toUpdate.addOrUpdate(FeatureClass, featureClass, system, identityToken);
		}
		if (featureCodes != null)
		{
			toUpdate.addOrUpdate(FeatureCodes, featureCodes, system, identityToken);
		}
		if (population != null)
		{
			toUpdate.addOrUpdate(Population, Integer.toString(population), system, identityToken);
		}
		if (elevation != null)
		{
			toUpdate.addOrUpdate(Elevation, Integer.toString(elevation), system, identityToken);
		}
		if (dEM != null)
		{
			toUpdate.addOrUpdate(DEM, Integer.toString(dEM), system, identityToken);
		}
		
		return toUpdate;
	}
}
