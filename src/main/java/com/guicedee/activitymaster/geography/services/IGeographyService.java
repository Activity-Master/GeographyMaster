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

	/**
	 * Downloads the GeoNames reference data set and the per-country geo-data and postal-code archives
	 * for the given country into the user's home directory (see {@code geodata.GeoDataLocation}).
	 *
	 * <p>The download runs on a worker thread so it never blocks the reactive event loop.</p>
	 *
	 * @param countryCode   the ISO-3166 alpha-2 country code (e.g. {@code "ZA"})
	 * @param identityToken optional security identity token(s)
	 * @return a {@link Uni} that completes when the files are present on disk
	 */
	Uni<Void> downloadCountryData(String countryCode, UUID... identityToken);

	// ---- Stateless twins ----
	Uni<IGeography<?, ?>> createPlanet(Mutiny.StatelessSession session, @NotNull String value, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken);
	Uni<IGeography<?, ?>> createContinent(Mutiny.StatelessSession session, String planetName, GeographyContinent continent, ISystems<?, ?> originatingSystem, String originalUniqueID, UUID... identityToken);
	Uni<IGeography<?, ?>> findPlanet(Mutiny.StatelessSession session, String name, ISystems<?, ?> originatingSystem, UUID... identityToken);
	Uni<GeographyContinent> findContinent(Mutiny.StatelessSession session, GeographyContinent continent, ISystems<?, ?> originatingSystem, UUID... identityToken);
	Uni<Void> loadProvincesASCII1(Mutiny.StatelessSession session, ISystems<?, ?> system, String countryCode, UUID... identityToken);
	Uni<Void> loadDistrictsASCII2(Mutiny.StatelessSession session, ISystems<?, ?> system, String countryCode, UUID... identityToken);
	Uni<Void> loadLanguages(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken);
	Uni<Void> loadCountryInfo(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken);
	Uni<GeographyCountry> findCountry(Mutiny.StatelessSession session, GeographyCountry country, ISystems<?, ?> system, UUID... identityToken);
	Uni<GeographyCountry> findCountryDetailed(Mutiny.StatelessSession session, String iso, ISystems<?, ?> system, UUID... identityToken);
	Uni<GeographyTimezone> findTimezone(Mutiny.StatelessSession session, GeographyTimezone timezone, ISystems<?, ?> system, UUID... identityToken);
	Uni<Void> loadTimeZones(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken);
	Uni<Void> loadPostalCodes(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken);
	Uni<GeographyPostalCode> findPostalCode(Mutiny.StatelessSession session, GeographyPostalCode postalCode, ISystems<?, ?> system, UUID... identityToken);
	Uni<GeographyPostalCode> findPostalCodeSuburb(Mutiny.StatelessSession session, String code, String description, ISystems<?, ?> system, UUID... identityToken);
	Uni<GeographyPostalCode> findOrCreatePostalCodeSuburb(Mutiny.StatelessSession session, String code, String description, ISystems<?, ?> system, UUID... identityToken);
	Uni<IGeography<?, ?>> findGeographyById(Mutiny.StatelessSession session, UUID geographyID, ISystems<?, ?> system, UUID... identityToken);
	Uni<Void> loadFeatureCodes(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken);
	Uni<GeographyFeatureCode> findFeatureCode(Mutiny.StatelessSession session, String featureCode, ISystems<?, ?> system, UUID... identityToken);
	Uni<IClassification<?, ?>> findFeatureCodeClassification(Mutiny.StatelessSession session, String featureCode, ISystems<?, ?> system, UUID... identityToken);
	Uni<Void> loadTownsAndCities(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken);
	Uni<Void> installCountry(Mutiny.StatelessSession session, ISystems<?, ?> system, String countryCode, UUID... identityToken);
	Uni<Void> loadCountryGeoData(Mutiny.StatelessSession session, ISystems<?, ?> system, String countryCode, UUID... identityToken);
	Uni<Void> loadCountryPostalCodes(Mutiny.StatelessSession session, ISystems<?, ?> system, String countryCode, UUID... identityToken);

	/**
	 * Lists the enterprise's active countries (ISO 3166 alpha-2 code and name), ordered by name.
	 * Empty until country info has been loaded.
	 */
	Uni<java.util.List<GeographyCountry>> listCountries(Mutiny.StatelessSession session, ISystems<?, ?> system, UUID... identityToken);

	/**
	 * Distinct names of active towns and cities within the given country whose name starts with {@code prefix}
	 * (case-insensitive), ordered by name. Empty unless that country's geodata has been installed.
	 *
	 * @param countryIso ISO 3166 alpha-2 code
	 * @param prefix     at least one character; LIKE wildcards are matched literally
	 * @param limit      1..100
	 */
	Uni<java.util.List<String>> searchPlaces(Mutiny.StatelessSession session, String countryIso, String prefix, int limit,
	                                         ISystems<?, ?> system, UUID... identityToken);
}
