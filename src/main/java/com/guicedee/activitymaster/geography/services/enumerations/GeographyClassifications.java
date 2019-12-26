package com.guicedee.activitymaster.geography.services.enumerations;

import com.guicedee.activitymaster.core.services.classifications.geography.IGeographyClassification;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationDataConceptValue;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationValue;

import static com.guicedee.activitymaster.core.services.concepts.EnterpriseClassificationDataConcepts.*;

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

	//GeoData
	FeatureCodes("A Feature that pertains to a geo item", GeographyXClassification),
	FeatureClass("A Feature class that pertains to a geo item", GeographyXClassification),
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

	//CountryData

	//#ISO	ISO3	ISO-Numeric	fips	Country	Capital	Area(in sq km)	Population	Continent	tld	CurrencyCode	CurrencyName	Phone	Postal Code Format	Postal Code Regex	Languages	geonameid	neighbours	EquivalentFipsCode



	;

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
