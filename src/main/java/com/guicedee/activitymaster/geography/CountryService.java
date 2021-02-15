package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.ClassificationService;
import com.guicedee.activitymaster.core.services.dto.IClassification;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationValue;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;

import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.get;
import static com.guicedee.guicedinjection.json.StaticStrings.STRING_EMPTY;

public class CountryService
{
	public static final Set<IClassificationValue<?>> CountryClassifications = Set.of(CountryISO3166, CountryISO3166_3, CountryISO_Numeric, CountryFips, CountryCapital, CountryAreaInSqKm, CountryTld, CountryPhone, CountryPostalCodeFormat, CountryPostalCodeRegex);
	
	@CacheResult(cacheName = "GeographyCountry",skipGet = true)
	public IGeography<?> createCountry(IGeography<?> continent, @CacheKey @NotNull String iso, @NotNull String description, String originalUniqueID,
	                                   @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Country, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(iso)
		                                .withClassification(classification)
		                                .inActiveRange(system, identityToken)
		                                .inDateRange()
		                                .withEnterprise(system)
		                                .getCount() > 0;
		if (exists)
		{
			return findCountry(iso, system, identityToken);
		}
		
		Geography geo = new Geography();
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassification(classification);
		geo.setSystemID((Systems) system);
		geo.setOriginalSourceSystemID((Systems) system);
		geo.setName(iso);
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
		continent.addChild(geo, system, identityToken);
		
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyCountry")
	public IGeography<?> findCountry(@CacheKey @NotNull String iso, @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Country, system, identityToken);
		return new Geography().builder()
		                      .withName(iso)
		                      .withClassification(classification)
		                      .inActiveRange(system, identityToken)
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find country - " + iso));
	}
	
	@CacheResult(cacheName = "GeographyCountry",skipGet = true)
	public IGeography<?> updateCountry(IClassification<?> currency, @CacheKey @NotNull String iso, @NotNull String description, String iso3, String isoNumeric,
	                                   String dialCode, String fips, String capital, String areaSqlKM, String postalCodeFormat, String postalCodeRegex, Integer population, String webTld,
	                                   @CacheKey ISystems<?> system, @CacheKey UUID... identityToken)
	{
		Geography geo = (Geography) findCountry(iso, system, identityToken);
		if(iso != null)
		geo.addOrUpdate(CountryISO3166, iso, system, identityToken);
		if (iso3 != null)
		{ geo.addOrUpdate(CountryISO3166_3, iso3, system, identityToken); }
		if (isoNumeric != null)
		{ geo.addOrUpdate(CountryISO_Numeric, isoNumeric, system, identityToken); }
		if (fips != null)
		{ geo.addOrUpdate(CountryFips, fips, system, identityToken); }
		if (capital != null)
		{ geo.addOrUpdate(CountryCapital, capital, system, identityToken); }
		if (areaSqlKM != null)
		{ geo.addOrUpdate(CountryAreaInSqKm, areaSqlKM, system, identityToken); }
		if (webTld != null)
		{ geo.addOrUpdate(CountryTld, webTld, system, identityToken); }
		if (population != null)
		{ geo.addOrUpdate(Population, population.toString(), system, identityToken); }
		if (dialCode != null)
		{ geo.addOrUpdate(CountryPhone, dialCode, system, identityToken); }
		if (postalCodeFormat != null)
		{ geo.addOrUpdate(CountryPostalCodeFormat, postalCodeFormat, system, identityToken); }
		if (postalCodeRegex != null)
		{ geo.addOrUpdate(CountryPostalCodeRegex, postalCodeRegex, system, identityToken); }
		
		if (currency != null)
		{ geo.addOrUpdate(currency, STRING_EMPTY, system, identityToken); }
		
		return geo;
	}
	
}
