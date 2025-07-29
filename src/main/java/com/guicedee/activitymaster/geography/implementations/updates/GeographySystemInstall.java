package com.guicedee.activitymaster.geography.implementations.updates;

import com.guicedee.activitymaster.fsdm.client.services.*;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.party.IInvolvedPartyIdentificationType;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts;
import com.guicedee.activitymaster.fsdm.client.services.systems.*;
import com.guicedee.activitymaster.geography.implementations.GeographySystem;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import io.smallrye.mutiny.Uni;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyIPIdentificationTypes;
import lombok.extern.log4j.Log4j2;

import static com.guicedee.client.IGuiceContext.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;

@SortedUpdate(sortOrder = 1000, taskCount = 12)
@Log4j2
public class GeographySystemInstall
		implements ISystemUpdate
{
	@Override
	public Uni<Boolean> update(Mutiny.Session session, IEnterprise<?,?> enterprise)
	{
		log.info("Starting geography system installation");
		
		// Get services and system information
		IClassificationService<?> classificationService = get(IClassificationService.class);
		IClassificationDataConceptService<?> dataConceptService = get(IClassificationDataConceptService.class);
		GeographySystem gs = get(GeographySystem.class);
		ISystems<?,?> system = gs.getSystem(enterprise);
		UUID token = gs.getSystemToken(enterprise);
		
		logProgress("Geography Master", "Creating Regional Areas");
		
		// Create base classifications sequentially
		return classificationService.create(session, Planet, system)
			.chain(() -> classificationService.create(session, Languages, system, Planet))
			.chain(() -> classificationService.create(session, Continent, system, Planet))
			.chain(() -> classificationService.create(session, Currency, system, Planet))
			.chain(() -> classificationService.create(session, TimeZone, system, Planet))
			
			// Create geographic hierarchy classifications
			.chain(() -> classificationService.create(session, Country, system, Continent))
			.chain(() -> classificationService.create(session, Province, system, Country))
			.chain(() -> classificationService.create(session, Location, system, Country))
			.chain(() -> classificationService.create(session, PostalCode, system, Province))
			.chain(() -> classificationService.create(session, PostalCodeSuburb, system, PostalCode))
			.chain(() -> classificationService.create(session, Municipalities, system, Province))
			.chain(() -> classificationService.create(session, City, system, Municipalities))
			.chain(() -> classificationService.create(session, Town, system, City))
			
			.chain(() -> {
				logProgress("Geography Master", "Creating Default Geography Classifications");
				
				// Create geography-specific classifications
				return classificationService.create(session, GeographyClassifications, system);
			})
			.chain(() -> classificationService.create(session, FeatureCodes, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Admin1CodeASCII, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Admin2Code, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Admin3Code, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Admin4Code, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, AdminCode5, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Population, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Elevation, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, DEM, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Name, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, NameAscii, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, AlternateNames, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Latitude, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, Longitude, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, CountryCode, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, CountryCode2, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, ContinentCode, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, GeoNameID, system, GeographyClassifications))
			
			// Create feature class
			.chain(() -> classificationService.create(session, FeatureClass, system, GeographyClassifications))
			.chain(classification -> {
				// Create feature class classifications
				Uni<IClassification<?, ?>> current = Uni.createFrom().item(classification);
				
				// Chain the creation of all feature class classifications
				for (GeographyFeatureClassesClassifications value : GeographyFeatureClassesClassifications.values()) {
					current = current.chain(c -> 
						classificationService.create(
							session,
							value.toString(), 
							value.classificationDescription(),
							EnterpriseClassificationDataConcepts.GeographyXClassification,
							system,
							0,
							classification, 
							identityToken
						)
					);
				}
				
				return current;
			})
			.chain(() -> classificationService.create(session, GeographyAdmin1AsciiCodes, system, GeographyClassifications))
			.chain(() -> classificationService.create(session, GeographyAsciiName, system, GeographyAdmin1AsciiCodes))
			
			.chain(() -> {
				logProgress("Geography Master", "Creating Geography Involved Parties");
				// Create Identification Type
				IInvolvedPartyService<?> involvedPartyService = get(IInvolvedPartyService.class);
				return involvedPartyService.createIdentificationType(
					session,
					system, 
					GeographyIPIdentificationTypes.ISP,
					"An Internet Service Provider",
					token
				);
			})
			
			.chain(idType -> {
				logProgress("Geography Master", "Creating Planets");
				// Create Planets and Continents by default
				IGeographyService<?> service = get(IGeographyService.class);
				return service.createPlanet(session, "Earth", null, system, identityToken);
			})
			
			.chain(planet -> {
				logProgress("Geography Master", "Creating Continents");
				IGeographyService<?> service = get(IGeographyService.class);
				
				// Chain the creation of all continents
				return service.createContinent(session, "Earth", new GeographyContinent().setContinentName("Africa")
													 .setContinentCode("AF"), system, "6255146", identityToken)
					.chain(() -> service.createContinent(session, "Earth", new GeographyContinent().setContinentName("Asia")
													 .setContinentCode("AS"), system, "6255147", identityToken))
					.chain(() -> service.createContinent(session, "Earth", new GeographyContinent().setContinentName("North America")
													 .setContinentCode("NA"), system, "6255149", identityToken))
					.chain(() -> service.createContinent(session, "Earth", new GeographyContinent().setContinentName("Europe")
													 .setContinentCode("EU"), system, "6255148", identityToken))
					.chain(() -> service.createContinent(session, "Earth", new GeographyContinent().setContinentName("Oceania")
													 .setContinentCode("OC"), system, "6255151", identityToken))
					.chain(() -> service.createContinent(session, "Earth", new GeographyContinent().setContinentName("South America")
													 .setContinentCode("SA"), system, "6255150", identityToken))
					.chain(() -> service.createContinent(session, "Earth", new GeographyContinent().setContinentName("Antarctica")
													 .setContinentCode("AN"), system, "6255152", identityToken));
			})
			
			.chain(() -> {
				logProgress("Geography Master", "Creating Feature Classes");
				return geonamesClassifications(session, enterprise);
			})
			.map(v -> true)
			.onFailure().invoke(error -> log.error("Error during geography system installation: {}", error.getMessage(), error))
			.onItem().invoke(() -> log.info("Geography system installation completed successfully"));
	}

	private Uni<Void> geonamesClassifications(Mutiny.Session session, IEnterprise<?,?> enterprise)
	{
		log.info("Creating geonames classifications");
		IClassificationService<?> classificationService = get(IClassificationService.class);
		GeographySystem gs = get(GeographySystem.class);
		ISystems<?,?> system = gs.getSystem(enterprise);

		// Chain all classification creation operations sequentially
		return classificationService.create(session, GeographyAsciiName, system, Country)
			.chain(() -> classificationService.create(session, GeographyAdmin2Codes, system, City))
			
			// ISO and country info classifications
			.chain(() -> classificationService.create(session, CountryISO3166, system, Country))
			.chain(() -> classificationService.create(session, CountryISO3166_3, system, Country))
			.chain(() -> classificationService.create(session, CountryISO_Numeric, system, Country))
			.chain(() -> classificationService.create(session, CountryFips, system, Country))
			.chain(() -> classificationService.create(session, CountryCapital, system, Country))
			.chain(() -> classificationService.create(session, CountryAreaInSqKm, system, Country))
			.chain(() -> classificationService.create(session, CountryTld, system, Country))
			.chain(() -> classificationService.create(session, CurrencyCode, system, Country))
			.chain(() -> classificationService.create(session, CurrencyName, system, Country))
			.chain(() -> classificationService.create(session, CountryPhone, system, Country))
			.chain(() -> classificationService.create(session, CountryPostalCodeFormat, system, Country))
			.chain(() -> classificationService.create(session, CountryPostalCodeRegex, system, Country))
			.chain(() -> classificationService.create(session, CountryNeighbours, system, Country))
			.chain(() -> classificationService.create(session, CountryEquivalentFipsCode, system, Country))
			
			// Timezone classifications
			.chain(() -> classificationService.create(session, TimeZoneOffsetJan2016, system, TimeZone))
			.chain(() -> classificationService.create(session, TimeZoneOffsetJuly2016, system, TimeZone))
			.chain(() -> classificationService.create(session, TimeZoneRawOffset, system, TimeZone))
			
			// Postal code classifications
			.chain(() -> classificationService.create(session, PostalNumber, system, PostalCode))
			.chain(() -> classificationService.create(session, PostalPlaceName, system, PostalCode))
			.onItem().invoke(() -> log.info("Geonames classifications created successfully"))
			.onFailure().invoke(error -> log.error("Error creating geonames classifications: {}", error.getMessage(), error))
			.replaceWithVoid();
	}
}