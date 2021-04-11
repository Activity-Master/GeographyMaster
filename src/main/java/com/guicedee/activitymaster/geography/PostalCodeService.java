package com.guicedee.activitymaster.geography;

import com.google.common.base.Strings;
import com.guicedee.activitymaster.fsdm.ClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.*;
import com.guicedee.activitymaster.fsdm.db.entities.geography.builders.*;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.logger.LogFactory;
import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.validation.constraints.NotNull;

import java.text.NumberFormat;
import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

public class PostalCodeService
{
	public static final Set<String> PostalCodeClassifications = Set.of(Latitude.toString(), Longitude.toString());
	
	private static final NumberFormat postalCodeFormat = NumberFormat.getInstance();
	
	static
	{
		postalCodeFormat.setGroupingUsed(false);
		postalCodeFormat.setMaximumFractionDigits(0);
		postalCodeFormat.setMinimumIntegerDigits(4);
	}
	
	@CacheResult(cacheName = "GeographyPostalCodes", skipGet = true)
	public IGeography<?,?> createPostalCode(@CacheKey IGeography<Geography, GeographyQueryBuilder> town, @NotNull @CacheKey String code,
	                                        String description, String originalUniqueID,
	                                        @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCode, system, identityToken);
		
		code = postalCodeFormat.format(Integer.parseInt(code));
		boolean exists = new Geography().builder()
		                                .withName(code)
		                                .withClassification(classification)
		                                .inActiveRange()
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
		
		town.addChild(geo,NoClassification.toString(),null, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyPostalCodeSuburbs", skipGet = true)
	public IGeography<Geography, GeographyQueryBuilder> createPostalCodeSuburb(@CacheKey IGeography<Geography, GeographyQueryBuilder> postalCode, @NotNull @CacheKey String code,
	                                            @NotNull @CacheKey String description, String originalUniqueID,
	                                            @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCodeSuburb, system, identityToken);
		code = postalCodeFormat.format(Integer.parseInt(code));
		
		boolean exists = new Geography().builder()
		                                .withName(code)
		                                .withDescription(description)
		                                .withClassification(classification)
		                                .inActiveRange()
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
		
		postalCode.addChild(geo,NoClassification.toString(),null, system, identityToken);
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyPostalCodes")
	public IGeography<Geography, GeographyQueryBuilder> findPostalCode(@CacheKey IGeography<Geography, GeographyQueryBuilder> town, @NotNull @CacheKey String code, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCode, system, identityToken);
		
		return new Geography().builder()
		                      .withName(code)
		                      .withClassification(classification)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find postal code in town - " + town + " - " + code));
	}
	
	@CacheResult(cacheName = "GeographyPostalCodesByNumber")
	public IGeography<Geography, GeographyQueryBuilder> findPostalCodeSuburb(@NotNull @CacheKey String code, @CacheKey String description, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCodeSuburb, system, identityToken);
		
		return new Geography().builder()
		                      .withName(code)
		                      .withDescription(description)
		                      .withClassification(classification)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find postal code suburb " + " - " + code));
	}
	
	@CacheResult(cacheName = "GeographyPostalCodesByNumber")
	public IGeography<Geography, GeographyQueryBuilder> findOrCreatePostalCodeSuburb(@NotNull @CacheKey String code, @CacheKey String description, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(PostalCodeSuburb, system, identityToken);
		
		Geography geography = new Geography().builder()
		                                     .withName(code)
		                                     .withDescription(description)
		                                     .withClassification(classification)
		                                     .inActiveRange()
		                                     .inDateRange()
		                                     .withEnterprise(system)
		                                     .get()
		                                     .orElse(null);
		if (geography != null)
		{
			return geography;
		}
		
		IGeography<?,?> postalCode = findPostalCode(null, code, system, identityToken);
		if (postalCode == null)
		{
			//create town
			LogFactory.getLog(getClass())
			          .warning("Unable to find postal code! - " + code);
		}
		IGeography<Geography, GeographyQueryBuilder> postalCodeSuburb = createPostalCodeSuburb((IGeography<Geography, GeographyQueryBuilder>) postalCode,
				code, description,
				null, system, identityToken);
		return postalCodeSuburb;
	}
	
	@SuppressWarnings("DuplicatedCode")
	@CacheResult(cacheName = "GeographyPostalCodes", skipGet = true)
	public IGeography<Geography, GeographyQueryBuilder> updatePostalCode(String districtCode, String townCode, @NotNull @CacheKey String code,
	                                      String description, String latitude, String longitude,
	                                      @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		IGeography<Geography, GeographyQueryBuilder> toUpdate = null;
		if (!Strings.isNullOrEmpty(districtCode))
		{
			IGeography<?,?> district = GuiceContext.get(DistrictService.class)
			                                     .findDistrict(districtCode, system, identityToken);
			IGeography<?,?> town = GuiceContext.get(TownService.class)
			                                 .findTown(district, townCode, system, identityToken);
			toUpdate = findPostalCode((IGeography<Geography, GeographyQueryBuilder>) town, code, system, identityToken);
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
			toUpdate.addOrUpdateClassification(Latitude, latitude, system, identityToken);
		}
		if (longitude != null)
		{
			toUpdate.addOrUpdateClassification(Longitude, longitude, system, identityToken);
		}
		return toUpdate;
	}
	
	
	@SuppressWarnings("DuplicatedCode")
	@CacheResult(cacheName = "GeographyPostalCodes", skipGet = true)
	public IGeography<Geography, GeographyQueryBuilder> updatePostalCodeParent(@NotNull @CacheKey String code,
	                                            String description, String latitude, String longitude,
	                                            @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		IGeography<Geography, GeographyQueryBuilder> toUpdate = null;
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
			toUpdate.addOrUpdateClassification(Latitude, latitude, system, identityToken);
		}
		if (longitude != null)
		{
			toUpdate.addOrUpdateClassification(Longitude, longitude, system, identityToken);
		}
		return toUpdate;
	}
}
