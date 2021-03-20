package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.client.services.IClassificationDataConceptService;
import com.guicedee.activitymaster.client.services.IClassificationService;
import com.guicedee.activitymaster.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.classifications.ClassificationDataConcept;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;

import java.util.UUID;

import static com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;


public class CurrencyService
{
	@CacheResult(cacheName = "GeographyCurrencies", skipGet = true)
	public IClassification<?,?> createCurrency(@CacheKey String code, String description, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ClassificationDataConcept currencyConcept = (ClassificationDataConcept) conceptService.find(ClassificationXClassification, system, identityToken);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		
		boolean exists = new Classification().builder()
		                                     .findByNameAndConcept(code, currencyConcept)
		                                     .inActiveRange(system, identityToken)
		                                     .inDateRange()
		                                     .withEnterprise(system)
		                                     .getCount() > 0;
		if (exists)
		{
			return findCurrency(code, system, identityToken);
		}
		
		return classificationService.create(code, description,
				ClassificationXClassification,
				system, 0,
				Currency.toString(),
				identityToken);
	}
	
	@CacheResult(cacheName = "GeographyCurrencies")
	public IClassification<?,?> findCurrency(@CacheKey String code, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ClassificationDataConcept currencyConcept = (ClassificationDataConcept) conceptService.find(ClassificationXClassification, system, identityToken);
		
		return new Classification().builder()
		                           .findByNameAndConcept(code, currencyConcept)
		                           .inActiveRange(system, identityToken)
		                           .inDateRange()
		                           .withEnterprise(system)
		                           .get()
		                           .orElseThrow(() -> new GeographyException("Cannot find currency with code : " + code));
	}
	
	@CacheResult(cacheName = "GeographyCurrencies", skipGet = true)
	public IClassification<?,?> updateCurrency(@CacheKey String code, String description, @CacheKey ISystems<?,?> system, @CacheKey UUID... identityToken)
	{
		IClassification<?,?> toUpdate = findCurrency(code, system, identityToken);
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
