package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.implementations.ClassificationService;
import com.guicedee.activitymaster.core.services.dto.IClassification;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.IGeography;
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

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.get;
import static com.guicedee.guicedinjection.json.StaticStrings.STRING_EMPTY;

@Singleton
public class CountryService
{
	public static final Set<IClassificationValue<?>> CountryClassifications = Set.of(CountryISO3166, CountryISO3166_3, CountryISO_Numeric, CountryFips, CountryCapital, CountryAreaInSqKm, CountryTld, CountryPhone, CountryPostalCodeFormat, CountryPostalCodeRegex);
	
	@CacheResult(cacheName = "GeographyCountry",skipGet = true)
	public IGeography<?> createCountry(IGeography<?> continent, @CacheKey @NotNull String iso, @NotNull String description, String originalUniqueID,
	                                   @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Country, enterprise, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(iso)
		                                .withClassification(classification)
		                                .inActiveRange(enterprise, identityToken)
		                                .inDateRange()
		                                .withEnterprise(enterprise)
		                                .getCount() > 0;
		if (exists)
		{
			return findCountry(iso, enterprise, identityToken);
		}
		
		Geography geo = new Geography();
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassification(classification);
		geo.setSystemID((Systems) geoSystem);
		geo.setOriginalSourceSystemID((Systems) geoSystem);
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
			geo.createDefaultSecurity(geoSystem, identityToken);
		}
		continent.addChild(geo, enterprise, identityToken);
		
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyCountry")
	public IGeography<?> findCountry(@CacheKey @NotNull String iso, @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Country, enterprise, identityToken);
		return new Geography().builder()
		                      .withName(iso)
		                      .withClassification(classification)
		                      .inActiveRange(enterprise, identityToken)
		                      .inDateRange()
		                      .withEnterprise(enterprise)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find country - " + iso));
	}
	
	@CacheResult(cacheName = "GeographyCountry",skipGet = true)
	public IGeography<?> updateCountry(IClassification<?> currency, @CacheKey @NotNull String iso, @NotNull String description, String iso3, String isoNumeric,
	                                   String dialCode, String fips, String capital, String areaSqlKM, String postalCodeFormat, String postalCodeRegex, Integer population, String webTld,
	                                   @CacheKey IEnterprise<?> enterprise, @CacheKey UUID... identityToken)
	{
		Geography geo = (Geography) findCountry(iso, enterprise, identityToken);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		
		if(iso != null)
		geo.addOrUpdate(CountryISO3166, iso, geoSystem, identityToken);
		if (iso3 != null)
		{ geo.addOrUpdate(CountryISO3166_3, iso3, geoSystem, identityToken); }
		if (isoNumeric != null)
		{ geo.addOrUpdate(CountryISO_Numeric, isoNumeric, geoSystem, identityToken); }
		if (fips != null)
		{ geo.addOrUpdate(CountryFips, fips, geoSystem, identityToken); }
		if (capital != null)
		{ geo.addOrUpdate(CountryCapital, capital, geoSystem, identityToken); }
		if (areaSqlKM != null)
		{ geo.addOrUpdate(CountryAreaInSqKm, areaSqlKM, geoSystem, identityToken); }
		if (webTld != null)
		{ geo.addOrUpdate(CountryTld, webTld, geoSystem, identityToken); }
		if (population != null)
		{ geo.addOrUpdate(Population, population.toString(), geoSystem, identityToken); }
		if (dialCode != null)
		{ geo.addOrUpdate(CountryPhone, dialCode, geoSystem, identityToken); }
		if (postalCodeFormat != null)
		{ geo.addOrUpdate(CountryPostalCodeFormat, postalCodeFormat, geoSystem, identityToken); }
		if (postalCodeRegex != null)
		{ geo.addOrUpdate(CountryPostalCodeRegex, postalCodeRegex, geoSystem, identityToken); }
		
		if (currency != null)
		{ geo.addOrUpdate(currency, STRING_EMPTY, geoSystem, identityToken); }
		
		return geo;
	}
	
}
