package com.guicedee.activitymaster.geography.services;

import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.systems.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.geography.services.dto.*;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public interface IGeographyService<J extends IGeographyService<J>>
{
	String GeographySystemName = "Geography System";

	Uni<IGeography<?, ?>> createPlanet(Mutiny.Session session, @NotNull String value, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken);

	Uni<IGeography<?, ?>> createContinent(Mutiny.Session session, String planetName, GeographyContinent continent, ISystems<?, ?> originatingSystem, String originalUniqueID, UUID... identityToken);

	Uni<IGeography<?, ?>> findPlanet(Mutiny.Session session, String name, ISystems<?, ?> originatingSystem, UUID... identityToken);

	Uni<GeographyContinent> findContinent(Mutiny.Session session, GeographyContinent continent, ISystems<?, ?> originatingSystem, UUID... identityToken);

	Uni<Void> loadProvincesASCII1(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken);

	Uni<Void> loadDistrictsASCII2(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken);

	Uni<Void> loadLanguages(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken);

	Uni<Void> loadCountryInfo(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken);

	Uni<GeographyCountry> findCountry(Mutiny.Session session, GeographyCountry country, ISystems<?, ?> system, UUID... identityToken);

	Uni<GeographyTimezone> findTimezone(Mutiny.Session session, GeographyTimezone timezone, ISystems<?, ?> system, UUID... identityToken);

	Uni<Void> loadTimeZones(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken);

	Uni<Void> loadPostalCodes(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken);

	Uni<GeographyPostalCode> findPostalCode(Mutiny.Session session, GeographyPostalCode postalCode, ISystems<?, ?> system, UUID... identityToken);

	Uni<GeographyPostalCode> findPostalCodeSuburb(Mutiny.Session session, String code, String description, ISystems<?, ?> system, UUID... identityToken);

	Uni<GeographyPostalCode> findOrCreatePostalCodeSuburb(Mutiny.Session session, String code, String description, ISystems<?, ?> system, UUID... identityToken);

	Uni<IGeography<?, ?>> findGeographyById(Mutiny.Session session, UUID geographyID, ISystems<?, ?> system, UUID... identityToken);

	Uni<Void> loadFeatureCodes(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken);

	Uni<GeographyFeatureCode> findFeatureCode(Mutiny.Session session, String featureCode, ISystems<?, ?> system, UUID... identityToken);

	Uni<IClassification<?, ?>> findFeatureCodeClassification(Mutiny.Session session, String featureCode, ISystems<?, ?> system, UUID... identityToken);

	Uni<Void> loadTownsAndCities(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken);
}
