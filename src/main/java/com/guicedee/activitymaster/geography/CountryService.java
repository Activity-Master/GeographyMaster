package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.fsdm.ClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.annotations.ActivityMasterDB;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.fsdm.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedpersistence.db.annotations.Transactional;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import jakarta.validation.constraints.NotNull;

import java.util.Set;


import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.json.StaticStrings.*;

public class CountryService
{
	public static final Set<String> CountryClassifications = Set.of(CountryISO3166.toString(),
			CountryISO3166_3.toString(),
			CountryISO_Numeric.toString(),
			CountryFips.toString(),
			CountryCapital.toString(),
			CountryAreaInSqKm.toString(),
			CountryTld.toString(),
			CountryPhone.toString(),
			CountryPostalCodeFormat.toString(),
			CountryPostalCodeRegex.toString());
	
	@CacheResult(cacheName = "GeographyCountry",skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IGeography<Geography, GeographyQueryBuilder> createCountry(IGeography<Geography, GeographyQueryBuilder> continent, @CacheKey @NotNull String iso, @NotNull String description, String originalUniqueID,
	                                     @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Country, system, identityToken);
		
		boolean exists = new Geography().builder()
		                                .withName(iso)
		                                .withClassification(classification)
		                                .inActiveRange()
		                                .inDateRange()
		                                .withEnterprise(system)
		                                .getCount() > 0;
		if (exists)
		{
			return findCountry(iso, system, identityToken);
		}
		
		Geography geo = new Geography();
		geo.setEnterpriseID(classification.getEnterpriseID());
		geo.setClassificationID(classification);
		geo.setSystemID(system);
		geo.setOriginalSourceSystemID(system);
		geo.setName(iso);
		geo.setDescription(description);
		if (originalUniqueID != null)
		{
			geo.setOriginalSourceSystemUniqueID(originalUniqueID);
		}
		geo.setActiveFlagID(classification.getActiveFlagID());
		geo.persist();
		
			geo.createDefaultSecurity(system, identityToken);
		
		continent.addChild(geo, DefaultClassifications.NoClassification.toString(),null, system, identityToken);
		
		return geo;
	}
	
	@CacheResult(cacheName = "GeographyCountry")
	public IGeography<Geography, GeographyQueryBuilder> findCountry(@CacheKey @NotNull String iso, @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Country, system, identityToken);
		return new Geography().builder()
		                      .withName(iso)
		                      .withClassification(classification)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(system)
		                      .get()
		                      .orElseThrow(() -> new GeographyException("Cannot find country - " + iso));
	}
	
	@CacheResult(cacheName = "GeographyCountry",skipGet = true)
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public IGeography<Geography, GeographyQueryBuilder> updateCountry(IClassification<?,?> currency, @CacheKey @NotNull String iso, @NotNull String description, String iso3, String isoNumeric,
	                                     String dialCode, String fips, String capital, String areaSqlKM, String postalCodeFormat, String postalCodeRegex, Integer population, String webTld,
	                                     @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		Geography geo = (Geography) findCountry(iso, system, identityToken);
		if(iso != null)
		geo.addOrUpdateClassification(CountryISO3166, iso,iso, system, identityToken);
		if (iso3 != null)
		{ geo.addOrUpdateClassification(CountryISO3166_3, iso3,iso3, system, identityToken); }
		if (isoNumeric != null)
		{ geo.addOrUpdateClassification(CountryISO_Numeric, isoNumeric,isoNumeric, system, identityToken); }
		if (fips != null)
		{ geo.addOrUpdateClassification(CountryFips, fips,fips, system, identityToken); }
		if (capital != null)
		{ geo.addOrUpdateClassification(CountryCapital, capital,capital, system, identityToken); }
		if (areaSqlKM != null)
		{ geo.addOrUpdateClassification(CountryAreaInSqKm, areaSqlKM,areaSqlKM, system, identityToken); }
		if (webTld != null)
		{ geo.addOrUpdateClassification(CountryTld, webTld,webTld, system, identityToken); }
		if (population != null)
		{ geo.addOrUpdateClassification(Population, population.toString(),population.toString(), system, identityToken); }
		if (dialCode != null)
		{ geo.addOrUpdateClassification(CountryPhone, dialCode,dialCode, system, identityToken); }
		if (postalCodeFormat != null)
		{ geo.addOrUpdateClassification(CountryPostalCodeFormat, postalCodeFormat,postalCodeFormat, system, identityToken); }
		if (postalCodeRegex != null)
		{ geo.addOrUpdateClassification(CountryPostalCodeRegex, postalCodeRegex, postalCodeRegex, system, identityToken); }
		
		if (currency != null)
		{ geo.addOrUpdateClassification(currency.toString(), STRING_EMPTY,STRING_EMPTY, system, identityToken); }
		
		return geo;
	}
	
}
