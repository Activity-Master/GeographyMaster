package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.annotations.ActivityMasterDB;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedpersistence.db.annotations.Transactional;
import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;

import java.util.Set;


import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;

public class TimeZoneService
{
	public static final Set<String> TimeZoneClassifications = Set.of(TimeZone.toString(),
			TimeZoneRawOffset.toString(),
			TimeZoneOffsetJuly2016.toString(),
			TimeZoneOffsetJan2016.toString());
	
	@CacheResult(cacheName = "GeographyTimezones", skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IClassification<?,?> createTimeZone(@CacheKey String code, String description, String originalUniqueID, @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		boolean exists = new Classification().builder()
		                                     .withName(code)
		                                     .withConcept(TimeZone.concept(),system,identityToken)
		                                     .inActiveRange()
		                                     .inDateRange()
		                                     .withEnterprise(system)
		                                     .getCount() > 0;
		if(exists)
		{
			return findTimeZone(code, system, identityToken);
		}
		return classificationService.create(code, description,
				EnterpriseClassificationDataConcepts.Classification,
		                                    system, 0,
		                                    identityToken);
	}
	
	@CacheResult(cacheName = "GeographyTimezones")
	public IClassification<?,?> findTimeZone(@CacheKey String code, @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		return new Classification().builder()
		                           .withName(code)
		                           .withConcept(TimeZone.concept(),system,identityToken)
		                           .inActiveRange()
		                           .inDateRange()
		                           .withEnterprise(system)
		                           .get()
		                           .orElseThrow(() -> new GeographyException("Unable to find timezone with code - " + code));
	}
	
	@CacheResult(cacheName = "GeographyTimezones", skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IClassification<?,?> updateTimeZone(@CacheKey String code, String description,
	                                         String timeZoneRawOffset, String timeZoneOffsetJuly2016, String timeZoneOffsetJan2016,
	                                         @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		IClassification<?,?> toUpdate = findTimeZone(code, system, identityToken);
		if (description != null)
		{
			Classification update = new Classification();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		if (timeZoneRawOffset != null)
		{
			toUpdate.addOrUpdateClassification(TimeZoneRawOffset, timeZoneRawOffset, system, identityToken);
		}
		if (timeZoneOffsetJuly2016 != null)
		{
			toUpdate.addOrUpdateClassification(TimeZoneOffsetJuly2016, timeZoneOffsetJuly2016, system, identityToken);
		}
		if (timeZoneOffsetJan2016 != null)
		{
			toUpdate.addOrUpdateClassification(TimeZoneOffsetJan2016, timeZoneOffsetJan2016, system, identityToken);
		}
		
		return toUpdate;
	}
}
