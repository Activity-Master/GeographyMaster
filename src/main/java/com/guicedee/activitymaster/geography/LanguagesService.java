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

@Singleton
public class LanguagesService
{
	public static final Set<IClassificationValue<?>> LanguagesClassifications = Set.of(ISO639_1, ISO639_2, ISO6392EnglishName, ISO6392FrenchName, ISO6392GermanName);
	
	@CacheResult(cacheName = "GeographyLanguages",
	             skipGet = true)
	public IClassification<?> createLanguage(@NotNull @CacheKey String code, String description, String originalUniqueID,
	                                         @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		boolean exists = new Classification().builder()
		                                     .withName(code)
		                                     .inActiveRange(enterprise, identityToken)
		                                     .inDateRange()
		                                     .withEnterprise(enterprise)
		                                     .getCount() > 0;
		if (exists)
		{
			return findLanguage(code, enterprise, identityToken);
		}
		IClassification<?> classification = classificationService.find(Languages, enterprise, identityToken);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		return classificationService.create(code, description, Languages.concept(), geoSystem, (short) 0, classification, identityToken);
	}
	
	@CacheResult(cacheName = "GeographyLanguages")
	public IClassification<?> findLanguage(@NotNull @CacheKey String code,
	                                       @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		return new Classification().builder()
		                           .withName(code)
		                           .inActiveRange(enterprise, identityToken)
		                           .inDateRange()
		                           .withEnterprise(enterprise)
		                           .get()
		                           .orElseThrow(() -> new GeographyException("Cannot find language - " + code));
	}
	@CacheResult(cacheName = "GeographyLanguages",
	             skipGet = true)
	public IClassification<?> updateLanguage(@NotNull @CacheKey String code, String description,
	                                         String iso_2, String englishName, String frenchName, String germanName,
	                                         @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		IClassification<?> toUpdate = findLanguage(code, enterprise, identityToken);
		if (description != null)
		{
			Classification update = new Classification();
			update.setId(toUpdate.getId());
			update.setDescription(description);
			update.update();
		}
		
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		if(iso_2 != null)
		{
			toUpdate.addOrReuse(ISO639_2, iso_2, geoSystem, identityToken);
		}
		if(englishName != null)
		{
			toUpdate.addOrReuse(ISO6392EnglishName, englishName, geoSystem, identityToken);
		}
		if(frenchName != null)
		{
			toUpdate.addOrReuse(ISO6392FrenchName, frenchName, geoSystem, identityToken);
		}
		if(germanName != null)
		{
			toUpdate.addOrReuse(ISO6392GermanName, germanName, geoSystem, identityToken);
		}
		return toUpdate;
	}
}
