package com.guicedee.activitymaster.geography.services.enumerations;

import static com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts.*;

public enum GeographyClassifications
{
	Planet("A Planet", GeographyXClassification),
	Languages("A Planets Languages", GeographyXClassification),
	Continent("Designates a continent", GeographyXGeography),
	Country("Designates a Country", GeographyXGeography),
	Municipalities("Designates a Municipal Area", GeographyXGeography),
	Currency("A specific currency", ClassificationXClassification),
	PostalCode("A postal code", GeographyXGeography),
	PostalCodeSuburb("A postal code suburb identifier", GeographyXGeography),
	Province("A Province", GeographyXGeography),
	City("A City", GeographyXGeography),
	Town("A Town", GeographyXGeography),
	Location("An identified location", Geography),
	TimeZone("A TimeZone", GeographyXGeography),

	GeographyClassifications("Parent for multi level classification hierarchies", GeographyXClassification),

	//GeoData Default fields
	Admin1CodeASCII("fipscode (subject to change to iso code), see exceptions below, see file admin1Codes.txt for display names of this code; varchar(20)",
			GeographyXClassification),
	Admin2Code("code for the second administrative division, a county in the US,", GeographyXClassification),
	Admin3Code("code for third level administrative division", GeographyXClassification),
	Admin4Code("code for fourth level administrative division", GeographyXClassification),
	AdminCode5("An updated code designated as admin 2", GeographyXClassification),
	Population("Area noted population", GeographyXClassification),
	Elevation("Area noted elevation", GeographyXClassification),
	DEM("digital elevation model, srtm3 or gtopo30, average elevation of 3''x3'' (ca 90mx90m) or 30''x30'' (ca 900mx900m) area in meters, integer. srtm processed by cgiar/ciat",
	    GeographyXClassification),
	Name("name of geographical point (utf8)", GeographyXClassification),
	NameAscii("name of geographical point in plain ascii characters", GeographyXClassification),
	AlternateNames("alternatenames, comma separated, ascii names automatically transliterated, convenience attribute from alternatename table", GeographyXClassification),
	Latitude("latitude in decimal degrees (wgs84)", GeographyXClassification),
	Longitude("longitude in decimal degrees (wgs84)", GeographyXClassification),
	CountryCode("ISO-3166 2-letter country code, 2 characters", GeographyXClassification),
	CountryCode2("alternate country codes, comma separated, ISO-3166 2-letter country code, 200 characters", GeographyXClassification),

	//LevelData
	ContinentCode("ISO-3166 2-letter continent code, 2 characters", GeographyXClassification),

	GeoNameID("The original GeoName ID for the given record", GeographyXClassification),

	//Lookups & Codes
	GeographyAdmin1AsciiCodes("Names in English for admin divisions", GeographyXClassification),
	GeographyAdmin2Codes("code for the second administrative division, a county in the US, see file admin2Codes.txt; varchar(80) ", GeographyXClassification),
	GeographyAsciiName("name of geographical point in plain ascii characters, varchar(200)", GeographyXClassification),

	FeatureCodes("A Feature that pertains to a geo item", GeographyXClassification),
	FeatureClass("A Feature class that pertains to a geo item", GeographyXClassification),
	//CountryData

	CountryISO3166("The two character code for a country", GeographyXClassification),
	CountryISO3166_3("The 3 character code for a country", GeographyXClassification),
	CountryISO_Numeric("The numeric number of a country", GeographyXClassification),
	CountryFips("The fips code for a country", GeographyXClassification),
	CountryCapital("The capital for a country", GeographyXClassification),
	CountryAreaInSqKm("The Area known in Square Kilometers", GeographyXClassification),
	CountryTld("The TLD for a country", GeographyXClassification),
	CurrencyCode("The currency code for a given country", GeographyXClassification),
	CurrencyName("The currency name for a given country", GeographyXClassification),
	CountryPhone("The phone code for a country", GeographyXClassification),
	CountryPostalCodeFormat("The postal code format for a given country", GeographyXClassification),
	CountryPostalCodeRegex("The given regex format for a country", GeographyXClassification),
	CountryNeighbours("The neighbours of a given country", GeographyXClassification),
	CountryEquivalentFipsCode("The equivalent fips code for a country", GeographyXClassification),

	TimeZoneOffsetJan2016("The offset of the timezone from January 2016", GeographyXClassification),
	TimeZoneOffsetJuly2016("The offset of the timezone from July 2016", GeographyXClassification),
	TimeZoneRawOffset("The raw offset of the timezone", GeographyXClassification),

	PostalNumber("The actual number for a postal code", GeographyXClassification),
	PostalPlaceName("The name for a postal code", GeographyXClassification),

	;

	private String description;
	private com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts concept;

	GeographyClassifications(String description, com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts concept)
	{
		this.description = description;
		this.concept = concept;
	}

	public String classificationDescription()
	{
		return this.description;
	}

	public com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts concept()
	{
		return concept;
	}
}
