package com.guicedee.activitymaster.geography;

import com.google.inject.Singleton;
import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.IActivityMasterSystem;
import com.guicedee.activitymaster.core.services.dto.IClassificationDataConcept;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.IInvolvedPartyIdentificationType;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.system.ActivityMasterDefaultSystem;
import com.guicedee.activitymaster.core.services.system.IClassificationDataConceptService;
import com.guicedee.activitymaster.core.services.system.IClassificationService;
import com.guicedee.activitymaster.core.services.system.IInvolvedPartyService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyIPIdentificationTypes;

import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;

@Singleton
public class GeographySystem
		extends ActivityMasterDefaultSystem<GeographySystem>
		implements IActivityMasterSystem<GeographySystem>
{
	@Override
	public void createDefaults(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{

	}

	@Override
	public int totalTasks()
	{
		return 0;
	}

	@Override
	public void loadUpdates(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{

		IClassificationService<?> classificationService = get(IClassificationService.class);
		IClassificationDataConceptService<?> dataConceptService = get(IClassificationDataConceptService.class);
		ISystems<?> system = getSystem(enterprise);
		UUID token = getSystemToken(enterprise);

		IClassificationDataConcept<?> dt = dataConceptService.createDataConcept(GeoNameClassificationDataConcept,
		                                                                        "Classifications relating to data from the geonames.org gazette",
		                                                                        system, token);

		IClassificationDataConcept<?> dtCurrency = dataConceptService.createDataConcept(GeographyCurrencyConcept,
		                                                                                "The known currencies of the world",
		                                                                                system, token);

		IClassificationDataConcept<?> dtTimeZones = dataConceptService.createDataConcept(GeographyTimezoneConcept,
		                                                                                 "The known Time Zones",
		                                                                                 system, token);
		IClassificationDataConcept<?> dtPostalCodes = dataConceptService.createDataConcept(GeographyPostalCodesConcept,
		                                                                                   "Postal codes related to a country",
		                                                                                   system, token);

		IClassificationDataConcept<?> dtCoordinates = dataConceptService.createDataConcept(GeographyCoordinatesConcept,
		                                                                                   "Co-ordinates for a planet",
		                                                                                   system, token);

		try
		{
			classificationService.find(Planet, enterprise, token);
		}
		catch (Exception e)
		{

			if (progressMonitor != null)

			{
				progressMonitor.progressUpdate("Geography Master", "Creating Regional Areas");
			}

			classificationService.create(Planet, system);
			classificationService.create(Languages, system,Planet);
			classificationService.create(Continent, system, Planet);
			classificationService.create(Country, system, Continent);
			classificationService.create(Currency, system, Country);
			classificationService.create(Province, system, Country);
			classificationService.create(PostalCode, system, Province);
			classificationService.create(PostalCodeSuburb, system, PostalCode);
			classificationService.create(Municipalities, system, Province);
			classificationService.create(City, system, Municipalities);
			classificationService.create(Town, system, City);
			classificationService.create(Location, system, Country);
			classificationService.create(TimeZone, system);

			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Geography Master", "Creating Default Geography Classifications");
			}

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
			classificationService.create(FeatureClass, system, GeographyClassifications);
			for (GeographyFeatureClassesClassifications value : GeographyFeatureClassesClassifications.values())
			{
				classificationService.create(value.classificationName(), value.classificationDescription(),
				                             GeoNameClassificationDataConcept,
				                             system,
				                             (short) 0,
				                             FeatureClass, token);
			}

			classificationService.create(GeographyAdmin1AsciiCodes, system, GeographyClassifications);
			classificationService.create(GeographyAsciiName, system, GeographyAdmin1AsciiCodes);

			//Country Data
			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Geography Master", "Creating Geography Involved Parties");
			}
			//Create Identification TYpe
			IInvolvedPartyService<?> involvedPartyService = get(IInvolvedPartyService.class);
			IInvolvedPartyIdentificationType<?> idType = involvedPartyService
					                                             .createIdentificationType(enterprise, GeographyIPIdentificationTypes.ISP,
					                                                                       "An Internet Service Provider",
					                                                                       token);
			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Loading Geography Updates", "Creating Planets");
			}
			//Create Planets and Continents by default
			GeographyService<?> service = (GeographyService<?>) get(IGeographyService.class);
			service.createPlanet("Earth",null, enterprise,token);
			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Loading Geography Updates", "Creating Continents");
			}

			service.createContinent("Earth", new GeographyContinent().setContinentName("Africa")
			                                                         .setContinentCode("AF"), system, "6255146", token);
			service.createContinent("Earth", new GeographyContinent().setContinentName("Asia")
			                                                         .setContinentCode("AS"), system, "6255147", token);
			service.createContinent("Earth", new GeographyContinent().setContinentName("North America")
			                                                         .setContinentCode("NA"), system, "6255149", token);
			service.createContinent("Earth", new GeographyContinent().setContinentName("Europe")
			                                                         .setContinentCode("EU"), system, "6255148", token);
			service.createContinent("Earth", new GeographyContinent().setContinentName("Oceania")
			                                                         .setContinentCode("OC"), system, "6255151", token);
			service.createContinent("Earth", new GeographyContinent().setContinentName("South America")
			                                                         .setContinentCode("SA"), system, "6255150", token);
			service.createContinent("Earth", new GeographyContinent().setContinentName("Antarctica")
			                                                         .setContinentCode("AN"), system, "6255152", token);
			//createInvolvedPartyClassifications(enterprise);
			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Loading Geography Updates", "Creating Feature Classes");
			}
		}

		//next update
		geonamesClassifications(enterprise);
	}

	@Override
	public String getSystemName()
	{
		return "Geography System";
	}

	@Override
	public String getSystemDescription()
	{
		return "The system for maintaining Geography and Locations";
	}

	private void geonamesClassifications(IEnterprise<?> enterprise)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		ISystems<?> system = getSystem(enterprise);
		classificationService.create(GeographyAsciiName, system, GeographyAdmin1AsciiCodes);
		classificationService.create(GeographyAdmin2Codes, system, GeographyClassifications);

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
		//timezones
		classificationService.create(TimeZoneOffsetJan2016, system, TimeZone);
		classificationService.create(TimeZoneOffsetJuly2016, system, TimeZone);
		classificationService.create(TimeZoneRawOffset, system, TimeZone);

		classificationService.create(PostalNumber, system, PostalCode);
		classificationService.create(PostalPlaceName, system, PostalCode);
	}
}
