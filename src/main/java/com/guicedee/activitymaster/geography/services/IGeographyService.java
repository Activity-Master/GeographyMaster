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
	
	void loadProvincesASCII1(ISystems<?> system, String countryCode, IActivityMasterProgressMonitor progressMonitor);
	
	void loadDistrictsASCII2(ISystems<?> system, String countryCode, IActivityMasterProgressMonitor progressMonitor);
	
	void loadLanguages(ISystems<?> system, IActivityMasterProgressMonitor progressMonitor);

	void loadCountryInfo(ISystems<?> system, IActivityMasterProgressMonitor progressMonitor);
	
	GeographyCountry findCountry(GeographyCountry country, ISystems<?> system, UUID... identityToken);

	GeographyTimezone findTimezone(GeographyTimezone timezone, ISystems<?> system);

	void loadTimeZones(ISystems<?> system, IActivityMasterProgressMonitor progressMonitor);

	void loadPostalCodes(ISystems<?> system, IActivityMasterProgressMonitor progressMonitor);
	
    GeographyPostalCode findPostalCode(GeographyPostalCode postalCode, ISystems<?> system, UUID... identityToken);
    
	GeographyPostalCode findPostalCodeSuburb(String code, String description, ISystems<?> system, UUID... identityToken);
	
	GeographyPostalCode findOrCreatePostalCodeSuburb(String code, String description, ISystems<?> system, UUID... identityToken);
	
	IGeography<?> findGeographyById(UUID geographyID, ISystems<?> system, UUID... identityToken);
	
	void loadFeatureCodes(ISystems<?> system, IActivityMasterProgressMonitor progressMonitor, UUID... identityToken);

    GeographyFeatureCode findFeatureCode(String featureCode, ISystems<?> system, UUID... identityToken);

	IClassification<?> findFeatureCodeClassification(String featureCode, ISystems<?> system, UUID... identityToken);
	
	void loadTownsAndCities(ISystems<?> system, IActivityMasterProgressMonitor progressMonitor);
}
