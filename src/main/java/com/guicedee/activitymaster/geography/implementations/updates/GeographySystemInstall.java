package com.guicedee.activitymaster.geography.implementations.updates;

import com.guicedee.activitymaster.fsdm.client.services.*;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts;
import com.guicedee.activitymaster.fsdm.client.services.systems.*;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.GeographySecurityCollector;
import io.smallrye.mutiny.Uni;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyIPIdentificationTypes;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

import static com.guicedee.client.IGuiceContext.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.InvolvedPartyClassifications.ISO639_1;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.InvolvedPartyClassifications.ISO639_2;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.InvolvedPartyClassifications.ISO6392EnglishName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.InvolvedPartyClassifications.ISO6392FrenchName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.InvolvedPartyClassifications.ISO6392GermanName;

@SortedUpdate(sortOrder = 1000, taskCount = 12)
@Log4j2
public class GeographySystemInstall
		implements ISystemUpdate
{
	@Override
	public Uni<Boolean> update(Mutiny.Session session, IEnterprise<?,?> enterprise)
	{
		log.info("Starting geography system installation");

		return SessionUtils.<Boolean>withActivityMaster(enterprise.getName(), IGeographyService.GeographySystemName, tuple -> {
			var amSession = tuple.getItem1();
			var amSystem = tuple.getItem3();
			var amToken = tuple.getItem4();

			IClassificationService<?> classificationService = get(IClassificationService.class);

			logProgress("Geography Master", "Creating Regional Areas");

			// Batch default security for the planet/continent rows (and any classification links) created
			// during this taxonomy install; flushed once below.
			get(GeographySecurityCollector.class).activate(amSession);

			// Create base classifications sequentially
			return classificationService.create(amSession, Planet, amSystem, (Enum<?>) null, amToken)
				.chain(() -> classificationService.create(amSession, Languages, amSystem, Planet, amToken))
				// Language-attribute classifications used by LanguagesService.updateLanguage (ISO 639 codes/names).
				// These are normally created by core's ClassificationBaseSetup, but geography loads languages
				// on demand and must be self-sufficient, so create them here as children of Languages. The
				// create() existence-check (name + concept + enterprise scoped) makes this idempotent when the
				// core base setup has already run.
				.chain(() -> classificationService.create(amSession, ISO639_1, amSystem, Languages, amToken))
				.chain(() -> classificationService.create(amSession, ISO639_2, amSystem, Languages, amToken))
				.chain(() -> classificationService.create(amSession, ISO6392EnglishName, amSystem, Languages, amToken))
				.chain(() -> classificationService.create(amSession, ISO6392FrenchName, amSystem, Languages, amToken))
				.chain(() -> classificationService.create(amSession, ISO6392GermanName, amSystem, Languages, amToken))
				.chain(() -> classificationService.create(amSession, Continent, amSystem, Planet, amToken))
				.chain(() -> classificationService.create(amSession, Currency, amSystem, Planet, amToken))
				.chain(() -> classificationService.create(amSession, TimeZone, amSystem, Planet, amToken))

				// Create geographic hierarchy classifications
				.chain(() -> classificationService.create(amSession, Country, amSystem, Continent, amToken))
				.chain(() -> classificationService.create(amSession, Province, amSystem, Country, amToken))
				.chain(() -> classificationService.create(amSession, Location, amSystem, Country, amToken))
				.chain(() -> classificationService.create(amSession, PostalCode, amSystem, Province, amToken))
				.chain(() -> classificationService.create(amSession, PostalCodeSuburb, amSystem, PostalCode, amToken))
				.chain(() -> classificationService.create(amSession, Municipalities, amSystem, Province, amToken))
				.chain(() -> classificationService.create(amSession, City, amSystem, Municipalities, amToken))
				.chain(() -> classificationService.create(amSession, Town, amSystem, City, amToken))

				.chain(() -> {
					logProgress("Geography Master", "Creating Default Geography Classifications");
					return classificationService.create(amSession, GeographyClassifications, amSystem, (Enum<?>) null, amToken);
				})
				.chain(() -> classificationService.create(amSession, FeatureCodes, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Admin1CodeASCII, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Admin2Code, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Admin3Code, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Admin4Code, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, AdminCode5, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Population, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Elevation, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, DEM, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Name, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, NameAscii, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, AlternateNames, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Latitude, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, Longitude, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, CountryCode, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, CountryCode2, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, ContinentCode, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, GeoNameID, amSystem, GeographyClassifications, amToken))

				// Create feature class
				.chain(() -> classificationService.create(amSession, FeatureClass, amSystem, GeographyClassifications, amToken))
				.chain(classification -> {
					Uni<IClassification<?, ?>> current = Uni.createFrom().item(classification);
					for (GeographyFeatureClassesClassifications value : GeographyFeatureClassesClassifications.values()) {
						current = current.chain(c ->
							classificationService.create(
								amSession,
								value.toString(),
								value.classificationDescription(),
								EnterpriseClassificationDataConcepts.GeographyXClassification,
								amSystem,
								0,
								classification,
								amToken
							)
						);
					}
					return current;
				})
				.chain(() -> classificationService.create(amSession, GeographyAdmin1AsciiCodes, amSystem, GeographyClassifications, amToken))
				.chain(() -> classificationService.create(amSession, GeographyAsciiName, amSystem, GeographyAdmin1AsciiCodes, amToken))

				.chain(() -> {
					logProgress("Geography Master", "Creating Geography Involved Parties");
					IInvolvedPartyService<?> involvedPartyService = get(IInvolvedPartyService.class);
					return involvedPartyService.createIdentificationType(
						amSession, amSystem, GeographyIPIdentificationTypes.ISP,
						"An Internet Service Provider", amToken
					);
				})

				.chain(idType -> {
					logProgress("Geography Master", "Creating Planets");
					IGeographyService<?> service = get(IGeographyService.class);
					return service.createPlanet(amSession, "Earth", null, amSystem, amToken);
				})

				.chain(planet -> {
					logProgress("Geography Master", "Creating Continents");
					IGeographyService<?> service = get(IGeographyService.class);
					return service.createContinent(amSession, "Earth", new GeographyContinent().setContinentName("Africa")
						.setContinentCode("AF"), amSystem, "6255146", amToken)
						.chain(() -> service.createContinent(amSession, "Earth", new GeographyContinent().setContinentName("Asia")
							.setContinentCode("AS"), amSystem, "6255147", amToken))
						.chain(() -> service.createContinent(amSession, "Earth", new GeographyContinent().setContinentName("North America")
							.setContinentCode("NA"), amSystem, "6255149", amToken))
						.chain(() -> service.createContinent(amSession, "Earth", new GeographyContinent().setContinentName("Europe")
							.setContinentCode("EU"), amSystem, "6255148", amToken))
						.chain(() -> service.createContinent(amSession, "Earth", new GeographyContinent().setContinentName("Oceania")
							.setContinentCode("OC"), amSystem, "6255151", amToken))
						.chain(() -> service.createContinent(amSession, "Earth", new GeographyContinent().setContinentName("South America")
							.setContinentCode("SA"), amSystem, "6255150", amToken))
						.chain(() -> service.createContinent(amSession, "Earth", new GeographyContinent().setContinentName("Antarctica")
							.setContinentCode("AN"), amSystem, "6255152", amToken));
				})

				.chain(() -> {
					logProgress("Geography Master", "Creating Feature Classes");
					return geonamesClassifications(amSession, amSystem, amToken);
				})
				// Secure the planet/continent geographies recorded during this taxonomy install in one batch.
				.chain(() -> get(GeographySecurityCollector.class).flush(amSession, amSystem, amToken))
				.map(v -> true);
		}).onFailure().invoke(error -> log.error("Error during geography system installation: {}", error.getMessage(), error))
		  .onItem().invoke(() -> log.info("Geography system installation completed successfully"));
	}

	private Uni<Void> geonamesClassifications(Mutiny.Session session, ISystems<?,?> system, UUID... identityToken)
	{
		log.info("Creating geonames classifications");
		IClassificationService<?> classificationService = get(IClassificationService.class);

		return classificationService.create(session, GeographyAsciiName, system, Country, identityToken)
			.chain(() -> classificationService.create(session, GeographyAdmin2Codes, system, City, identityToken))
			.chain(() -> classificationService.create(session, CountryISO3166, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryISO3166_3, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryISO_Numeric, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryFips, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryCapital, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryAreaInSqKm, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryTld, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CurrencyCode, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CurrencyName, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryPhone, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryPostalCodeFormat, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryPostalCodeRegex, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryNeighbours, system, Country, identityToken))
			.chain(() -> classificationService.create(session, CountryEquivalentFipsCode, system, Country, identityToken))
			.chain(() -> classificationService.create(session, TimeZoneOffsetJan2016, system, TimeZone, identityToken))
			.chain(() -> classificationService.create(session, TimeZoneOffsetJuly2016, system, TimeZone, identityToken))
			.chain(() -> classificationService.create(session, TimeZoneRawOffset, system, TimeZone, identityToken))
			.chain(() -> classificationService.create(session, PostalNumber, system, PostalCode, identityToken))
			.chain(() -> classificationService.create(session, PostalPlaceName, system, PostalCode, identityToken))
			.onItem().invoke(() -> log.info("Geonames classifications created successfully"))
			.onFailure().invoke(error -> log.error("Error creating geonames classifications: {}", error.getMessage(), error))
			.replaceWithVoid();
	}
}