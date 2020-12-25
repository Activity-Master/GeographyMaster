package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.classifications.ClassificationDataConcept;
import com.guicedee.activitymaster.core.services.dto.IClassification;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.system.IClassificationDataConceptService;
import com.guicedee.activitymaster.core.services.system.IClassificationService;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;

import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.inject.Singleton;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.GeographyCurrencyConcept;
import static com.guicedee.guicedinjection.GuiceContext.get;


public class CurrencyService
{
	@CacheResult(cacheName = "GeographyCurrencies",skipGet = true)
	public IClassification<?> createCurrency(@CacheKey String code,String description, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID...identityToken)
	{
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ClassificationDataConcept currencyConcept = (ClassificationDataConcept) conceptService.find(GeographyCurrencyConcept, enterprise, identityToken);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		
		boolean exists = new Classification().builder()
		                    .findByNameAndConcept(code, currencyConcept)
		                    .inActiveRange(enterprise, identityToken)
		                    .inDateRange()
		                    .withEnterprise(enterprise)
		                    .getCount() > 0;
		if(exists)
		{
			return findCurrency(code, enterprise, identityToken);
		}
		
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		return classificationService.create(code, description,
		                                    GeographyCurrencyConcept,
		                                    geoSystem, (short) 0,
		                                    identityToken);
	}
	
	@CacheResult(cacheName = "GeographyCurrencies")
	public IClassification<?> findCurrency(@CacheKey String code, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID...identityToken)
	{
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ClassificationDataConcept currencyConcept = (ClassificationDataConcept) conceptService.find(GeographyCurrencyConcept, enterprise, identityToken);
		
		return new Classification().builder()
		                           .findByNameAndConcept(code, currencyConcept)
		                           .inActiveRange(enterprise, identityToken)
		                           .inDateRange()
		                           .withEnterprise(enterprise)
		                           .get()
		                           .orElseThrow(() -> new GeographyException("Cannot find currency with code : " + code));
	}
	
	@CacheResult(cacheName = "GeographyCurrencies",skipGet = true)
	public IClassification<?> updateCurrency(@CacheKey String code,String description, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID...identityToken)
	{
		IClassification<?> toUpdate = findCurrency(code, enterprise, identityToken);
		if (description != null)
		{
			Classification update = new Classification();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		
		return toUpdate;
	}
}
