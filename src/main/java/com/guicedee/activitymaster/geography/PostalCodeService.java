package com.guicedee.activitymaster.geography;

import com.google.common.base.Strings;
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
import com.guicedee.logger.LogFactory;

import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

import java.text.NumberFormat;
import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.get;

public class PostalCodeService
{
	public static final Set<IClassificationValue<?>> PostalCodeClassifications = Set.of(Latitude, Longitude);
	
	private static final NumberFormat postalCodeFormat = NumberFormat.getInstance();
	
	static
	{
		postalCodeFormat.setGroupingUsed(false);
		postalCodeFormat.setMaximumFractionDigits(0);
		postalCodeFormat.setMinimumIntegerDigits(4);
	}
	
	@CacheResult(cacheName = "GeographyPostalCodes", skipGet = true)
	public IGeography<?> createPostalCode(@CacheKey IGeography<?> town, @NotNull @CacheKey String code,
	                                      String description, String originalUniqueID,
	                                      @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCode, system, identityToken);
		
		code = postalCodeFormat.format(Integer.parseInt(code));
		boolean exists = new Geography().builder()
		                                .withName(code)
		                                .withClassification(classification)
		                                .inActiveRange(system, identityToken)
		                                .inDateRange()
		                                .withEnterprise(system)
		                                .getCount() > 0;
		if (exists)
		{
			return findPostalCode(town, code, system, identityToken);
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
		town.addChild(geo, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyPostalCodeSuburbs", skipGet = true)
	public IGeography<?> createPostalCodeSuburb(@CacheKey IGeography<?> postalCode, @NotNull @CacheKey String code,
	                                            @NotNull @CacheKey String description, String originalUniqueID,
	                                            @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCodeSuburb, system, identityToken);
		code = postalCodeFormat.format(Integer.parseInt(code));
		
		boolean exists = new Geography().builder()
		                                .withName(code)
		                                .withDescription(description)
		                                .withClassification(classification)
		                                .inActiveRange(system, identityToken)
		                                .inDateRange()
		                                .withEnterprise(system)
		                                .getCount() > 0;
		if (exists)
		{
			return findPostalCodeSuburb(code, description, system, identityToken);
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
		postalCode.addChild(geo, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyPostalCodes")
	public IGeography<?> findPostalCode(@CacheKey IGeography<?> town, @NotNull @CacheKey String code, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCode, system, identityToken);
		
		return new Geography().builder()
		                      .withName(code)
		                      .withClassification(classification)
		                      .inActiveRange(system, identityToken)
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find postal code in town - " + town + " - " + code));
	}
	
	@CacheResult(cacheName = "GeographyPostalCodesByNumber")
	public IGeography<?> findPostalCodeSuburb(@NotNull @CacheKey String code, @CacheKey String description, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCodeSuburb, system, identityToken);
		
		return new Geography().builder()
		                      .withName(code)
		                      .withDescription(description)
		                      .withClassification(classification)
		                      .inActiveRange(system, identityToken)
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find postal code suburb " + " - " + code));
	}
	
	@CacheResult(cacheName = "GeographyPostalCodesByNumber")
	public IGeography<?> findOrCreatePostalCodeSuburb(@NotNull @CacheKey String code, @CacheKey String description, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCodeSuburb, system, identityToken);
		
		Geography geography = new Geography().builder()
		                                     .withName(code)
		                                     .withDescription(description)
		                                     .withClassification(classification)
		                                     .inActiveRange(system, identityToken)
		                                     .inDateRange()
		                                     .withEnterprise(system)
		                                     .get()
		                                     .orElse(null);
		if (geography != null)
		{
			return geography;
		}
		
		IGeography<?> postalCode = findPostalCode(null, code, system, identityToken);
		if (postalCode == null)
		{
			//create town
			LogFactory.getLog(getClass())
			          .warning("Unable to find postal code! - " + code);
		}
		IGeography<?> postalCodeSuburb = createPostalCodeSuburb(postalCode, code, description, null, system, identityToken);
		return postalCodeSuburb;
	}
	
	@SuppressWarnings("DuplicatedCode")
	@CacheResult(cacheName = "GeographyPostalCodes", skipGet = true)
	public IGeography<?> updatePostalCode(String districtCode, String townCode, @NotNull @CacheKey String code,
	                                      String description, String latitude, String longitude,
	                                      @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		IGeography<?> toUpdate = null;
		if (!Strings.isNullOrEmpty(districtCode))
		{
			IGeography<?> district = GuiceContext.get(DistrictService.class)
			                                     .findDistrict(districtCode, system, identityToken);
			IGeography<?> town = GuiceContext.get(TownService.class)
			                                 .findTown(district, townCode, system, identityToken);
			toUpdate = findPostalCode(town, code, system, identityToken);
		}
		else
		{
			toUpdate = findPostalCode(null, code, system, identityToken);
		}
		
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
		return toUpdate;
	}
	
	
	@SuppressWarnings("DuplicatedCode")
	@CacheResult(cacheName = "GeographyPostalCodes", skipGet = true)
	public IGeography<?> updatePostalCodeParent(@NotNull @CacheKey String code,
	                                            String description, String latitude, String longitude,
	                                            @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		IGeography<?> toUpdate = null;
		toUpdate = findPostalCode(null, code, system, identityToken);
		
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
		return toUpdate;
	}
}
