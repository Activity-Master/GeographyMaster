package com.guicedee.activitymaster.geography.services;

import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.classifications.enterprise.IEnterpriseName;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import java.util.UUID;

public interface IGeographyService<J extends IGeographyService<J>>
{
	IGeography<?> findPlanet(String name, ISystems<?> originatingSystem, UUID... identifyingToken);

	GeographyContinent findContinent(GeographyContinent continent, ISystems<?> originatingSystem, UUID... identifyingToken);

	void loadAsciiCodes(IEnterpriseName<?> enterpriseName, String countryCode, IActivityMasterProgressMonitor progressMonitor);

	void loadAdmin2Codes(IEnterpriseName<?> enterpriseName, String countryCode, IActivityMasterProgressMonitor progressMonitor);

	GeographyAsciiCode findAdmin1AsciiCode(GeographyAsciiCode asciiCode, IEnterpriseName<?> enterpriseName);

	GeographyAsciiCode findAdmin2Code(GeographyAsciiCode asciiCode, IEnterpriseName<?> enterpriseName);

	void loadLanguages(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor);

	@CacheResult(cacheName = "ISO639Languages")
	ISO639Language create(@CacheKey ISO639Language language, @CacheKey IEnterpriseName<?> enterpriseName);

	ISO639Language findLanguage(String asciiCode, IEnterpriseName<?> enterpriseName);
}
