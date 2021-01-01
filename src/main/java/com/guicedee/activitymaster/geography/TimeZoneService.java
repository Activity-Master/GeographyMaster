package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.classifications.ClassificationDataConcept;
import com.guicedee.activitymaster.core.services.dto.IClassification;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationValue;
import com.guicedee.activitymaster.core.services.system.IClassificationDataConceptService;
import com.guicedee.activitymaster.core.services.system.IClassificationService;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;

import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.GeographyCurrencyConcept;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.GeographyTimezoneConcept;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.get;

public class TimeZoneService
{
	public static final Set<IClassificationValue<?>> TimeZoneClassifications = Set.of(TimeZone,TimeZoneRawOffset, TimeZoneOffsetJuly2016,TimeZoneOffsetJan2016);
	
	@CacheResult(cacheName = "GeographyTimezones", skipGet = true)
	public IClassification<?> createTimeZone(@CacheKey String code, String description, String originalUniqueID, @CacheKey ISystems<?> system, @CacheKey UUID...identityToken)
	{
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ClassificationDataConcept currencyConcept = (ClassificationDataConcept) conceptService.find(GeographyTimezoneConcept, system, identityToken);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		
		boolean exists = new Classification().builder()
		                                     .findByNameAndConcept(code, currencyConcept)
		                                     .inActiveRange(system, identityToken)
		                                     .inDateRange()
		                                     .withEnterprise(system)
		                                     .getCount() > 0;
		if(exists)
		{
			return findTimeZone(code, system, identityToken);
		}
		return classificationService.create(code, description,
		                                    GeographyTimezoneConcept.toString(),
		                                    system, 0,
		                                    identityToken);
	}
	
	@CacheResult(cacheName = "GeographyTimezones")
	public IClassification<?> findTimeZone(@CacheKey String code, @CacheKey ISystems<?> system, @CacheKey UUID...identityToken)
	{
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ClassificationDataConcept timeZoneConcept = (ClassificationDataConcept) conceptService.find(GeographyTimezoneConcept, system, identityToken);
		return new Classification().builder()
		                           .findByNameAndConcept(code, timeZoneConcept)
		                           .inActiveRange(system, identityToken)
		                           .inDateRange()
		                           .withEnterprise(system)
		                           .get()
		                           .orElseThrow(() -> new GeographyException("Unable to find timezone with code - " + code));
	}
	
	@CacheResult(cacheName = "GeographyTimezones", skipGet = true)
	public IClassification<?> updateTimeZone(@CacheKey String code, String description,
	                                         String timeZoneRawOffset, String timeZoneOffsetJuly2016, String timeZoneOffsetJan2016,
	                                         @CacheKey ISystems<?> system, @CacheKey UUID...identityToken)
	{
		IClassification<?> toUpdate = findTimeZone(code, system, identityToken);
		if (description != null)
		{
			Classification update = new Classification();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		if (timeZoneRawOffset != null)
		{
			toUpdate.addOrUpdate(TimeZoneRawOffset, timeZoneRawOffset, system, identityToken);
		}
		if (timeZoneOffsetJuly2016 != null)
		{
			toUpdate.addOrUpdate(TimeZoneOffsetJuly2016, timeZoneOffsetJuly2016, system, identityToken);
		}
		if (timeZoneOffsetJan2016 != null)
		{
			toUpdate.addOrUpdate(TimeZoneOffsetJan2016, timeZoneOffsetJan2016, system, identityToken);
		}
		
		return toUpdate;
	}
}
