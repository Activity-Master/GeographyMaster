package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.fsdm.ClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.annotations.ActivityMasterDB;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
//import com.google.inject.persist.Transactional;


import jakarta.validation.constraints.NotNull;

import java.util.Set;


import static com.guicedee.activitymaster.fsdm.client.services.classifications.InvolvedPartyClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.Languages;

public class LanguagesService
{
	public static final Set<String> LanguagesClassifications = Set.of(ISO639_1.toString(),
			ISO639_2.toString(),
			ISO6392EnglishName.toString(),
			ISO6392FrenchName.toString(),
			ISO6392GermanName.toString());
	
	//@CacheResult(cacheName = "GeographyLanguages",
	             skipGet = true)
	////@Transactional()
	public IClassification<?,?> createLanguage(@NotNull  String code, String description, String originalUniqueID,
	                                            ISystems<?,?> system,  java.util.UUID... identityToken)
	{
		ClassificationService classificationService = com.guicedee.client.IGuiceContext.get(ClassificationService.class);
		boolean exists = new Classification().builder()
		                                     .withName(code)
		                                     .withConcept(Languages.concept(),system,identityToken)
		                                     .inActiveRange()
		                                     .inDateRange()
		                                     .withEnterprise(system)
		                                     .getCount() > 0;
		if (exists)
		{
			return findLanguage(code, system, identityToken);
		}
		IClassification<?,?> classification = classificationService.find(Languages, system, identityToken);
		return classificationService.create(code, description, Languages.concept(), system, 0, classification, identityToken);
	}
	
	//@CacheResult(cacheName = "GeographyLanguages")
	public IClassification<?,?> findLanguage(@NotNull  String code,
	                                        ISystems<?,?> system,  java.util.UUID... identityToken)
	{
		return new Classification().builder()
		                           .withName(code)
		                           .withConcept(Languages.concept(),system,identityToken)
		                           .inActiveRange()
		                           .inDateRange()
		                           .withEnterprise(system)
		                           .get()
		                           .orElseThrow(() -> new GeographyException("Cannot find language - " + code));
	}
	//@CacheResult(cacheName = "GeographyLanguages",
	             skipGet = true)
	////@Transactional()
	public IClassification<?,?> updateLanguage(@NotNull  String code, String description,
	                                         String iso_2, String englishName, String frenchName, String germanName,
	                                          ISystems<?,?> system,  java.util.UUID... identityToken)
	{
		IClassification<?,?> toUpdate = findLanguage(code, system, identityToken);
		if (description != null)
		{
			Classification update = new Classification();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		if(iso_2 != null)
		{
			toUpdate.addOrReuseClassification(ISO639_2, iso_2, system, identityToken);
		}
		if(englishName != null)
		{
			toUpdate.addOrReuseClassification(ISO6392EnglishName, englishName, system, identityToken);
		}
		if(frenchName != null)
		{
			toUpdate.addOrReuseClassification(ISO6392FrenchName, frenchName, system, identityToken);
		}
		if(germanName != null)
		{
			toUpdate.addOrReuseClassification(ISO6392GermanName, germanName, system, identityToken);
		}
		return toUpdate;
	}
}
