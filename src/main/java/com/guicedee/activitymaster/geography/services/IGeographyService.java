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

	/**
	 * Resolves a country by its ISO-3166 alpha-2 code and hydrates a fully-populated
	 * {@link GeographyCountry} DTO by reading the persisted warehouse {@code Geography} row and its
	 * supporting classifications (ISO3, numeric ISO, FIPS, capital, area, TLD, dialling code, postal
	 * formats, population and original GeoName id).
	 *
	 * <p>This is the canonical read path used by both the GraphQL data fetcher and the REST resource
	 * so the strongly-typed DTO returned by either transport reflects exactly what is stored in
	 * ActivityMaster.</p>
	 *
	 * @param session       the active reactive session
	 * @param iso           the ISO-3166 alpha-2 country code (e.g. {@code "ZA"})
	 * @param system        the requesting system (security scope)
	 * @param identityToken optional security identity token(s)
	 * @return a {@link Uni} emitting the hydrated {@link GeographyCountry}
	 */
	Uni<GeographyCountry> findCountryDetailed(Mutiny.Session session, String iso, ISystems<?, ?> system, UUID... identityToken);

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

	/**
	 * Installs a single country end-to-end: it ensures the GeoNames files are downloaded, then loads the
	 * provinces (admin1), districts (admin2), towns/cities (per-country geo-data) and postal codes into
	 * ActivityMaster.
	 *
	 * @param session       the active reactive session
	 * @param system        the requesting system (security scope)
	 * @param countryCode   the ISO-3166 alpha-2 country code (e.g. {@code "ZA"})
	 * @param identityToken optional security identity token(s)
	 * @return a {@link Uni} that completes when the country has been installed
	 */
	Uni<Void> installCountry(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken);

	/**
	 * Loads the per-country geo-data (towns, cities and other places) from the downloaded
	 * {@code {CC}.txt} file into ActivityMaster.
	 *
	 * @param session       the active reactive session
	 * @param system        the requesting system (security scope)
	 * @param countryCode   the ISO-3166 alpha-2 country code
	 * @param identityToken optional security identity token(s)
	 * @return a {@link Uni} that completes when the geo-data has been loaded
	 */
	Uni<Void> loadCountryGeoData(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken);

	/**
	 * Loads the per-country postal codes from the downloaded {@code {CC}.txt} file into ActivityMaster.
	 *
	 * @param session       the active reactive session
	 * @param system        the requesting system (security scope)
	 * @param countryCode   the ISO-3166 alpha-2 country code
	 * @param identityToken optional security identity token(s)
	 * @return a {@link Uni} that completes when the postal codes have been loaded
	 */
	Uni<Void> loadCountryPostalCodes(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken);

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
}
