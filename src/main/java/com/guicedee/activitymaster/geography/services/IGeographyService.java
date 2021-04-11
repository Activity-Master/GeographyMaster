package com.guicedee.activitymaster.geography.services;

import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.systems.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.geography.services.dto.*;
import jakarta.cache.annotation.CacheKey;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public interface IGeographyService<J extends IGeographyService<J>>
{
	String GeographySystemName = "Geography System";
	
	IGeography<?,?> createPlanet(@CacheKey @NotNull String value, String originalUniqueID, ISystems<?,?> system, UUID... identityToken);
	
	IGeography<?,?> createContinent(String planetName, GeographyContinent continent, ISystems<?,?> originatingSystem, String originalUniqueID, UUID... identityToken);
	
	IGeography<?,?> findPlanet(String name, ISystems<?,?> originatingSystem, UUID... identifyingToken);

	GeographyContinent findContinent(GeographyContinent continent, ISystems<?,?> originatingSystem, UUID... identifyingToken);
	
	void loadProvincesASCII1(ISystems<?,?> system, String countryCode);
	
	void loadDistrictsASCII2(ISystems<?,?> system, String countryCode);
	
	void loadLanguages(ISystems<?,?> system);

	void loadCountryInfo(ISystems<?,?> system);
	
	GeographyCountry findCountry(GeographyCountry country, ISystems<?,?> system, UUID... identityToken);

	GeographyTimezone findTimezone(GeographyTimezone timezone, ISystems<?,?> system);

	void loadTimeZones(ISystems<?,?> system);

	void loadPostalCodes(ISystems<?,?> system);
	
    GeographyPostalCode findPostalCode(GeographyPostalCode postalCode, ISystems<?,?> system, UUID... identityToken);
    
	GeographyPostalCode findPostalCodeSuburb(String code, String description, ISystems<?,?> system, UUID... identityToken);
	
	GeographyPostalCode findOrCreatePostalCodeSuburb(String code, String description, ISystems<?,?> system, UUID... identityToken);
	
	IGeography<?,?> findGeographyById(UUID geographyID, ISystems<?,?> system, UUID... identityToken);
	
	void loadFeatureCodes(ISystems<?,?> system, UUID... identityToken);

    GeographyFeatureCode findFeatureCode(String featureCode, ISystems<?,?> system, UUID... identityToken);

	IClassification<?,?> findFeatureCodeClassification(String featureCode, ISystems<?,?> system, UUID... identityToken);
	
	void loadTownsAndCities(ISystems<?,?> system);
}
