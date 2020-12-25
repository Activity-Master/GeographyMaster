package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
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

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.DEM;
import static com.guicedee.guicedinjection.GuiceContext.get;

public class TownService
{
	public static final Set<IClassificationValue<?>> TownClassifications = Set.copyOf(ProvinceService.ProvinceClassifications);
	
	public IGeography<?> createTown(@CacheKey IGeography<?> district,@CacheKey String name, String description, String originalUniqueID, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, enterprise, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(name)
		                                .withClassification(classification)
		                                .inActiveRange(enterprise, identityToken)
		                                .inDateRange()
		                                .withEnterprise(enterprise)
		                                .getCount() > 0;
		if (exists)
		{
			return findTown(district,name, enterprise, identityToken);
		}
		
		Geography geo = new Geography();
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassification(classification);
		geo.setSystemID((Systems) geoSystem);
		geo.setOriginalSourceSystemID((Systems) geoSystem);
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
			geo.createDefaultSecurity(geoSystem, identityToken);
		}
		district.addChild(geo, enterprise, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyTowns")
	public IGeography<?> findTown(@CacheKey IGeography<?> district, @CacheKey String name, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, enterprise, identityToken);
		
		return new Geography().builder()
		                      .withName(name)
		                      .withClassification(classification)
		                      .inActiveRange(enterprise, identityToken)
		                      .inDateRange()
		                      .withEnterprise(enterprise)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find town - " + name + " - in district - " + district));
	}
	
	@CacheResult(cacheName = "GeographyTownNames")
	public IGeography<?> findTown( @CacheKey String name, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Town, enterprise, identityToken);
		
		return new Geography().builder()
		                      .withName(name)
		                      .withClassification(classification)
		                      .inActiveRange(enterprise, identityToken)
		                      .inDateRange()
		                      .withEnterprise(enterprise)
		                      .setReturnFirst(true)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find town - " + name));
	}
	
	@SuppressWarnings("DuplicatedCode")
	@CacheResult(cacheName = "GeographyTowns", skipGet = true)
	public IGeography<?> updateTown(String districtCode, @NotNull @CacheKey String name, String description,
	                                    String latitude, String longitude, String featureCodes, String featureClass, Integer population, Integer elevation, Integer dEM,
	                                    @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		IGeography<?> district = GuiceContext.get(DistrictService.class)
		                                     .findDistrict(districtCode, enterprise, identityToken);
		
		IGeography<?> toUpdate = findTown(district, name, enterprise, identityToken);
		if (description != null)
		{
			Geography update = new Geography();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		if (latitude != null)
		{
			toUpdate.addOrUpdate(Latitude, latitude, geoSystem, identityToken);
		}
		if (longitude != null)
		{
			toUpdate.addOrUpdate(Longitude, longitude, geoSystem, identityToken);
		}
		if (featureClass != null)
		{
			toUpdate.addOrUpdate(FeatureClass, featureClass, geoSystem, identityToken);
		}
		if (featureCodes != null)
		{
			toUpdate.addOrUpdate(FeatureCodes, featureCodes, geoSystem, identityToken);
		}
		if (population != null)
		{
			toUpdate.addOrUpdate(Population, Integer.toString(population), geoSystem, identityToken);
		}
		if (elevation != null)
		{
			toUpdate.addOrUpdate(Elevation, Integer.toString(elevation), geoSystem, identityToken);
		}
		if (dEM != null)
		{
			toUpdate.addOrUpdate(DEM, Integer.toString(dEM), geoSystem, identityToken);
		}
		
		return toUpdate;
	}
}
