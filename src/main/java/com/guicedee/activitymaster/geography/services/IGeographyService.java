package com.guicedee.activitymaster.geography.services;

import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.classifications.enterprise.IEnterpriseName;
import com.guicedee.activitymaster.core.services.dto.IClassification;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.geography.services.dto.*;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;

import jakarta.cache.annotation.CacheKey;
import jakarta.cache.annotation.CacheResult;
import java.util.UUID;

public interface IGeographyService<J extends IGeographyService<J>>
{
	IGeography<?> findPlanet(String name, ISystems<?> originatingSystem, UUID... identifyingToken);

	GeographyContinent findContinent(GeographyContinent continent, ISystems<?> originatingSystem, UUID... identifyingToken);
	
	void loadProvincesASCII1(IEnterpriseName<?> enterpriseName, String countryCode, IActivityMasterProgressMonitor progressMonitor);
	
	void loadDistrictsASCII2(IEnterpriseName<?> enterpriseName, String countryCode, IActivityMasterProgressMonitor progressMonitor);
	
	void loadLanguages(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor);

	void loadCountryInfo(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor);
	
	GeographyCountry findCountry(GeographyCountry country, IEnterpriseName<?> enterpriseName, UUID... identityToken);

	GeographyTimezone findTimezone(GeographyTimezone timezone, IEnterpriseName<?> enterpriseName);

	void loadTimeZones(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor);

	void loadPostalCodes(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor);
	
    GeographyPostalCode findPostalCode(GeographyPostalCode postalCode, IEnterpriseName<?> enterpriseName, UUID... identityToken);
	
	@CacheResult(cacheName = "GeographyPostalCodesSuburb")
	GeographyPostalCode findPostalCodeSuburb(@CacheKey String code, @CacheKey String description, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken);
	
	@CacheResult(cacheName = "GeographyPostalCodesSuburb")
	GeographyPostalCode findOrCreatePostalCodeSuburb(@CacheKey String code, @CacheKey String description, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken);
	
	IGeography<?> findGeographyById(UUID geographyID, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken);
	
	void loadFeatureCodes(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor, UUID... identityToken);

    GeographyFeatureCode findFeatureCode(String featureCode, IEnterpriseName<?> enterpriseName, UUID... identityToken);

	IClassification<?> findFeatureCodeClassification(String featureCode, IEnterpriseName<?> enterpriseName, UUID... identityToken);
	
	void loadTownsAndCities(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor);
}
