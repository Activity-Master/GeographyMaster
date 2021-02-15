package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.ClassificationService;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationValue;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;

public class TownService
{
	public static final Set<IClassificationValue<?>> TownClassifications = Set.copyOf(ProvinceService.ProvinceClassifications);
	
	public IGeography<?> createTown(@CacheKey IGeography<?> district,@CacheKey String name, String description, String originalUniqueID, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(name)
		                                .withClassification(classification)
		                                .inActiveRange(system, identityToken)
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
		geo.setSystemID((Systems) system);
		geo.setOriginalSourceSystemID((Systems) system);
		geo.setName(name);
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
		district.addChild(geo, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyTowns")
	public IGeography<?> findTown(@CacheKey IGeography<?> district, @CacheKey String name, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, system, identityToken);
		
		return new Geography().builder()
		                      .withName(name)
		                      .withClassification(classification)
		                      .inActiveRange(system, identityToken)
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find town - " + name + " - in district - " + district));
	}
	
	@CacheResult(cacheName = "GeographyTownNames")
	public IGeography<?> findTown( @CacheKey String name, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, system, identityToken);
		
		return new Geography().builder()
		                      .withName(name)
		                      .withClassification(classification)
		                      .inActiveRange(system, identityToken)
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .setReturnFirst(true)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find town - " + name));
	}
	
	@SuppressWarnings("DuplicatedCode")
	@CacheResult(cacheName = "GeographyTowns", skipGet = true)
	public IGeography<?> updateTown(String districtCode, @NotNull @CacheKey String name, String description,
	                                    String latitude, String longitude, String featureCodes, String featureClass, Integer population, Integer elevation, Integer dEM,
	                                    @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		IGeography<?> district = GuiceContext.get(DistrictService.class)
		                                     .findDistrict(districtCode, system, identityToken);
		
		IGeography<?> toUpdate = findTown(district, name, system, identityToken);
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
