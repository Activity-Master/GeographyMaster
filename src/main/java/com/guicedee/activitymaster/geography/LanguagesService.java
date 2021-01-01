package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.implementations.ClassificationService;
import com.guicedee.activitymaster.core.services.dto.IClassification;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
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

import static com.guicedee.activitymaster.core.services.classifications.involvedparty.InvolvedPartyClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.Languages;
import static com.guicedee.guicedinjection.GuiceContext.get;

public class LanguagesService
{
	public static final Set<IClassificationValue<?>> LanguagesClassifications = Set.of(ISO639_1, ISO639_2, ISO6392EnglishName, ISO6392FrenchName, ISO6392GermanName);
	
	@CacheResult(cacheName = "GeographyLanguages",
	             skipGet = true)
	public IClassification<?> createLanguage(@NotNull @CacheKey String code, String description, String originalUniqueID,
	                                         @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		boolean exists = new Classification().builder()
		                                     .withName(code)
		                                     .inActiveRange(system, identityToken)
		                                     .inDateRange()
		                                     .withEnterprise(system)
		                                     .getCount() > 0;
		if (exists)
		{
			return findLanguage(code, system, identityToken);
		}
		IClassification<?> classification = classificationService.find(Languages, system, identityToken);
		return classificationService.create(code, description, Languages.concept().name(), system, 0, classification, identityToken);
	}
	
	@CacheResult(cacheName = "GeographyLanguages")
	public IClassification<?> findLanguage(@NotNull @CacheKey String code,
	                                       @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		return new Classification().builder()
		                           .withName(code)
		                           .inActiveRange(system, identityToken)
		                           .inDateRange()
		                           .withEnterprise(system)
		                           .get()
		                           .orElseThrow(() -> new GeographyException("Cannot find language - " + code));
	}
	@CacheResult(cacheName = "GeographyLanguages",
	             skipGet = true)
	public IClassification<?> updateLanguage(@NotNull @CacheKey String code, String description,
	                                         String iso_2, String englishName, String frenchName, String germanName,
	                                         @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		IClassification<?> toUpdate = findLanguage(code, system, identityToken);
		if (description != null)
		{
			Classification update = new Classification();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		if(iso_2 != null)
		{
			toUpdate.addOrReuse(ISO639_2, iso_2, system, identityToken);
		}
		if(englishName != null)
		{
			toUpdate.addOrReuse(ISO6392EnglishName, englishName, system, identityToken);
		}
		if(frenchName != null)
		{
			toUpdate.addOrReuse(ISO6392FrenchName, frenchName, system, identityToken);
		}
		if(germanName != null)
		{
			toUpdate.addOrReuse(ISO6392GermanName, germanName, system, identityToken);
		}
		return toUpdate;
	}
}
