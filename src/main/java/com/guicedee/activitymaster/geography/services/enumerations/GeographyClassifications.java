package com.guicedee.activitymaster.geography.services.enumerations;

import com.guicedee.activitymaster.core.services.classifications.geography.IGeographyClassification;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationDataConceptValue;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationValue;

import static com.guicedee.activitymaster.core.services.concepts.EnterpriseClassificationDataConcepts.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.*;

public enum GeographyClassifications
		implements IClassificationValue<GeographyClassifications>
				           , IGeographyClassification<GeographyClassifications>
{
	Planet("A Planet", GeographyXClassification),
	Languages("A Planets Languages", GlobalClassificationsDataConceptName),
	Continent("Designates a continent", GeographyXGeography),
	Country("Designates a Country", GeographyXGeography),
	Municipalities("Designates a Municipal Area", GeographyXGeography),
	Currency("A specific currency", GlobalClassificationsDataConceptName),
	PostalCode("A postal code", GeographyXGeography),
	Province("A Province", GeographyXGeography),
	City("A City", GeographyXGeography),
	Town("A Town", GeographyXGeography),
	TimeZone("A TimeZone", GeographyXGeography),

	GeographyClassifications("Parent for multi level classification hierarchies", GeographyXClassification),

	//GeoData Default fields
	Admin1CodeASCII("fipscode (subject to change to iso code), see exceptions below, see file admin1Codes.txt for display names of this code; varchar(20)",
	                GeoNameClassificationDataConcept),
	Admin2Code("code for the second administrative division, a county in the US,", GeoNameClassificationDataConcept),
	Admin3Code("code for third level administrative division", GeoNameClassificationDataConcept),
	Admin4Code("code for fourth level administrative division", GeoNameClassificationDataConcept),
	AdminCode5("An updated code designated as admin 2", GeoNameClassificationDataConcept),
	Population("Area noted population", GeoNameClassificationDataConcept),
	Elevation("Area noted elevation", GeoNameClassificationDataConcept),
	DEM("digital elevation model, srtm3 or gtopo30, average elevation of 3''x3'' (ca 90mx90m) or 30''x30'' (ca 900mx900m) area in meters, integer. srtm processed by cgiar/ciat",
	    GeoNameClassificationDataConcept),
	Name("name of geographical point (utf8)", GeographyXClassification),
	NameAscii("name of geographical point in plain ascii characters", GeoNameClassificationDataConcept),
	AlternateNames("alternatenames, comma separated, ascii names automatically transliterated, convenience attribute from alternatename table", GeoNameClassificationDataConcept),
	Latitude("latitude in decimal degrees (wgs84)", GeoNameClassificationDataConcept),
	Longitude("longitude in decimal degrees (wgs84)", GeoNameClassificationDataConcept),
	CountryCode("ISO-3166 2-letter country code, 2 characters", GeoNameClassificationDataConcept),
	CountryCode2("alternate country codes, comma separated, ISO-3166 2-letter country code, 200 characters", GeoNameClassificationDataConcept),

	//LevelData
	ContinentCode("ISO-3166 2-letter continent code, 2 characters", GeoNameClassificationDataConcept),

	GeoNameID("The original GeoName ID for the given record", GeoNameClassificationDataConcept),

	//Lookups & Codes
	GeographyAdmin1AsciiCodes("Names in English for admin divisions", GeoNameClassificationDataConcept),
	GeographyAdmin2Codes("code for the second administrative division, a county in the US, see file admin2Codes.txt; varchar(80) ", GeoNameClassificationDataConcept),
	GeographyAsciiName("name of geographical point in plain ascii characters, varchar(200)", GeoNameClassificationDataConcept),

	FeatureCodes("A Feature that pertains to a geo item", GeoNameClassificationDataConcept),
	FeatureClass("A Feature class that pertains to a geo item", GeoNameClassificationDataConcept),
	//CountryData

	CountryISO3166("The two character code for a country", GeoNameClassificationDataConcept),
	CountryISO3166_3("The 3 character code for a country", GeoNameClassificationDataConcept),
	CountryISO_Numeric("The numeric number of a country", GeoNameClassificationDataConcept),
	CountryFips("The fips code for a country", GeoNameClassificationDataConcept),
	CountryCapital("The capital for a country", GeoNameClassificationDataConcept),
	CountryAreaInSqKm("The Area known in Square Kilometers", GeoNameClassificationDataConcept),
	CountryTld("The TLD for a country", GeoNameClassificationDataConcept),
	CurrencyCode("The currency code for a given country", GeoNameClassificationDataConcept),
	CurrencyName("The currency name for a given country", GeoNameClassificationDataConcept),
	CountryPhone("The phone code for a country", GeoNameClassificationDataConcept),
	CountryPostalCodeFormat("The postal code format for a given country", GeoNameClassificationDataConcept),
	CountryPostalCodeRegex("The given regex format for a country", GeoNameClassificationDataConcept),
	CountryNeighbours("The neighbours of a given country", GeoNameClassificationDataConcept),
	CountryEquivalentFipsCode("The equivalent fips code for a country", GeoNameClassificationDataConcept);

	private String description;
	private IClassificationDataConceptValue<?> concept;

	GeographyClassifications(String description, IClassificationDataConceptValue<?> concept)
	{
		this.description = description;
		this.concept = concept;
	}

	@Override
	public String classificationDescription()
	{
		return this.description;
	}

	@Override
	public IClassificationDataConceptValue<?> concept()
	{
		return concept;
	}
}
