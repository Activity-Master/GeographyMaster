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
	public Uni<Boolean> update(IEnterprise<?,?> enterprise)
	{
		log.info("Starting geography system installation");

		return Uni.createFrom().item(() -> {
			IClassificationService<?> classificationService = get(IClassificationService.class);
			IClassificationDataConceptService<?> dataConceptService = get(IClassificationDataConceptService.class);

			GeographySystem gs = get(GeographySystem.class);
			ISystems<?,?> system = gs.getSystem(enterprise);
			UUID token = gs.getSystemToken(enterprise);

			logProgress("Geography Master", "Creating Regional Areas");

			// Create base classifications
			classificationService.create(Planet, system);
			classificationService.create(Languages, system, Planet);
			classificationService.create(Continent, system, Planet);
			classificationService.create(Currency, system, Planet);
			classificationService.create(TimeZone, system,Planet);

			// Create geographic hierarchy classifications
			classificationService.create(Country, system, Continent);
			classificationService.create(Province, system, Country);
			classificationService.create(Location, system, Country);

			classificationService.create(PostalCode, system, Province);
			classificationService.create(PostalCodeSuburb, system, PostalCode);
			classificationService.create(Municipalities, system, Province);
			classificationService.create(City, system, Municipalities);
			classificationService.create(Town, system, City);

			logProgress("Geography Master", "Creating Default Geography Classifications");

			// Create geography-specific classifications
			classificationService.create(GeographyClassifications, system);
			classificationService.create(FeatureCodes, system, GeographyClassifications);

			classificationService.create(Admin1CodeASCII, system, GeographyClassifications);
			classificationService.create(Admin2Code, system, GeographyClassifications);
			classificationService.create(Admin3Code, system, GeographyClassifications);
			classificationService.create(Admin4Code, system, GeographyClassifications);
			classificationService.create(AdminCode5, system, GeographyClassifications);
			classificationService.create(Population, system, GeographyClassifications);
			classificationService.create(Elevation, system, GeographyClassifications);
			classificationService.create(DEM, system, GeographyClassifications);
			classificationService.create(Name, system, GeographyClassifications);
			classificationService.create(NameAscii, system, GeographyClassifications);
			classificationService.create(AlternateNames, system, GeographyClassifications);
			classificationService.create(Latitude, system, GeographyClassifications);
			classificationService.create(Longitude, system, GeographyClassifications);
			classificationService.create(CountryCode, system, GeographyClassifications);
			classificationService.create(CountryCode2, system, GeographyClassifications);
			classificationService.create(ContinentCode, system, GeographyClassifications);
			classificationService.create(GeoNameID, system, GeographyClassifications);

			//Lookups for geography data
			//Static features classes
			IClassification<?, ?> classification = classificationService.create(FeatureClass, system, GeographyClassifications);
			for (GeographyFeatureClassesClassifications value : GeographyFeatureClassesClassifications.values())
			{
				classificationService.create(value.toString(), value.classificationDescription(),
						EnterpriseClassificationDataConcepts.GeographyXClassification,
						system,
						0,
						classification, identityToken);
			}

			classificationService.create(GeographyAdmin1AsciiCodes, system, GeographyClassifications);
			classificationService.create(GeographyAsciiName, system, GeographyAdmin1AsciiCodes);

			//Country Data
			logProgress("Geography Master", "Creating Geography Involved Parties");
			//Create Identification TYpe
			IInvolvedPartyService<?> involvedPartyService = get(IInvolvedPartyService.class);
			IInvolvedPartyIdentificationType<?,?> idType = involvedPartyService
					.createIdentificationType(system, GeographyIPIdentificationTypes.ISP,
							"An Internet Service Provider",
							token);

			logProgress("Geography Master", "Creating Planets");
			//Create Planets and Continents by default
			IGeographyService<?> service = get(IGeographyService.class);
			service.createPlanet("Earth", null, system, identityToken);

			logProgress("Geography Master", "Creating Continents");
			service.createContinent("Earth", new GeographyContinent().setContinentName("Africa")
													 .setContinentCode("AF"), system, "6255146", identityToken);
			service.createContinent("Earth", new GeographyContinent().setContinentName("Asia")
													 .setContinentCode("AS"), system, "6255147", identityToken);
			service.createContinent("Earth", new GeographyContinent().setContinentName("North America")
													 .setContinentCode("NA"), system, "6255149", identityToken);
			service.createContinent("Earth", new GeographyContinent().setContinentName("Europe")
													 .setContinentCode("EU"), system, "6255148", identityToken);
			service.createContinent("Earth", new GeographyContinent().setContinentName("Oceania")
													 .setContinentCode("OC"), system, "6255151", identityToken);
			service.createContinent("Earth", new GeographyContinent().setContinentName("South America")
													 .setContinentCode("SA"), system, "6255150", identityToken);
			service.createContinent("Earth", new GeographyContinent().setContinentName("Antarctica")
													 .setContinentCode("AN"), system, "6255152", identityToken);

			logProgress("Geography Master", "Creating Feature Classes");
			//next update
			return true;
		})
		.chain(() -> geonamesClassifications(enterprise).map(v -> true))
		.onFailure().invoke(error -> log.error("Error during geography system installation: {}", error.getMessage(), error))
		.onItem().invoke(() -> log.info("Geography system installation completed successfully"));
	}

	private Uni<Void> geonamesClassifications(IEnterprise<?,?> enterprise)
	{
		return Uni.createFrom().item(() -> {
			log.info("Creating geonames classifications");
			IClassificationService<?> classificationService = get(IClassificationService.class);
			GeographySystem gs = get(GeographySystem.class);
			ISystems<?,?> system = gs.getSystem(enterprise);

			// Country-related classifications
			classificationService.create(GeographyAsciiName, system, Country);
			classificationService.create(GeographyAdmin2Codes, system, City);

			// ISO and country info classifications
			classificationService.create(CountryISO3166, system, Country);
			classificationService.create(CountryISO3166_3, system, Country);
			classificationService.create(CountryISO_Numeric, system, Country);
			classificationService.create(CountryFips, system, Country);
			classificationService.create(CountryCapital, system, Country);
			classificationService.create(CountryAreaInSqKm, system, Country);
			classificationService.create(CountryTld, system, Country);
			classificationService.create(CurrencyCode, system, Country);
			classificationService.create(CurrencyName, system, Country);
			classificationService.create(CountryPhone, system, Country);
			classificationService.create(CountryPostalCodeFormat, system, Country);
			classificationService.create(CountryPostalCodeRegex, system, Country);
			classificationService.create(CountryNeighbours, system, Country);
			classificationService.create(CountryEquivalentFipsCode, system, Country);

			// Timezone classifications
			classificationService.create(TimeZoneOffsetJan2016, system, TimeZone);
			classificationService.create(TimeZoneOffsetJuly2016, system, TimeZone);
			classificationService.create(TimeZoneRawOffset, system, TimeZone);

			// Postal code classifications
			classificationService.create(PostalNumber, system, PostalCode);
			classificationService.create(PostalPlaceName, system, PostalCode);

			log.info("Geonames classifications created successfully");
			return null;
		})
		.onFailure().invoke(error -> log.error("Error creating geonames classifications: {}", error.getMessage(), error));
	}
}
