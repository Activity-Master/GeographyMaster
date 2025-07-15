package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.annotations.ActivityMasterDB;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
//import com.google.inject.persist.Transactional;





import static com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.client.IGuiceContext.*;


public class CurrencyService
{
	//@CacheResult(cacheName = "GeographyCurrencies", skipGet = true)
	////@Transactional()
	public IClassification<?,?> createCurrency( String code, String description,  ISystems<?,?> system,  java.util.UUID... identityToken)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		
		boolean exists = new Classification().builder()
		                                     .withName(code)
		                                     .withConcept(Currency.concept(),system,identityToken)
		                                     .inActiveRange()
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
	
	//@CacheResult(cacheName = "GeographyCurrencies")
	public IClassification<?,?> findCurrency( String code,  ISystems<?,?> system,  java.util.UUID... identityToken)
	{
		return new Classification().builder()
		                           .withName(code)
		                           .withConcept(Currency.concept(),system,identityToken)
		                           .inActiveRange()
		                           .inDateRange()
		                           .withEnterprise(system)
		                           .get()
		                           .orElseThrow(() -> new GeographyException("Cannot find currency with code : " + code));
	}
	
	//@CacheResult(cacheName = "GeographyCurrencies", skipGet = true)
	////@Transactional()
	public IClassification<?,?> updateCurrency( String code, String description,  ISystems<?,?> system,  java.util.UUID... identityToken)
	{
		IClassification<?,?> toUpdate = findCurrency(code, system, identityToken);
		if (description != null)
		{
			com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification update = new Classification();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		
		return toUpdate;
	}
}
