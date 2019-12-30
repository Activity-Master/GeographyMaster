package com.guicedee.activitymaster.geography.services;

import com.guicedee.activitymaster.core.services.classifications.enterprise.IEnterpriseName;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;

import java.util.List;
import java.util.UUID;

public interface IGeographyService<J extends IGeographyService<J>>
{
	IGeography<?> findPlanet(String name, ISystems<?> originatingSystem, UUID... identifyingToken);

	GeographyContinent findContinent(GeographyContinent continent, ISystems<?> originatingSystem, UUID... identifyingToken);

	List<GeographyAsciiCode> loadAsciiCodes(IEnterpriseName<?> enterpriseName);
}
