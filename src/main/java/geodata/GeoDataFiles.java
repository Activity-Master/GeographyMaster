package geodata;

public enum GeoDataFiles
{
	Admin1CodesASCII("admin1CodesASCII.csv", "code", "name", "name ascii", "geonameid"),
	Admin2Codes("admin2Codes.csv", "codes", "name", "asciiname", "geonamid"),
	AdminCode5("adminCode5.csv", "geonameid", "adm5code"),
	CountryInfo("countryInfo.csv", "ISO", "ISO3", "ISO-Numeric", "fips", "Country", "Capital", "Area(in sq km)", "Population", "Continent", "tld", "CurrencyCode", "CurrencyName",
	            "Phone", "Postal Code Format", "Postal Code Regex", "Languages", "geonameid", "neighbours", "EquivalentFipsCode"),
	FeatureCodes_en("featureCodes_en", "code", "description"),
	Hierarchy("hierarchy.csv", "parentId", "childId", "type"),
	ISOLanguageCodes("iso_languagecodes.csv", "ISO 639-3", "ISO 639-2", "ISO 639-1", "Language Name"),
	TimeZones("timeZones.csv", "CountryCode", "TimeZoneId", "GMT offset 1. Jan 2019", "DST offset 1. Jul 2019", "rawOffset"),
	ZAGeoData("ZA.csv", "geonameid", "name", "asciiname", "alternatenames", "latitude", "longitude", "feature class", "feature code", "country code", "cc2", "admin1 code",
	          "admin2 code", "admin3 code", "admin4 code", "population", "elevation", "dem", "timezone", "modification date"),
	ZAPostalCodes("ZA_PostalCodes.csv", "country code", "postal code", "place name", "admin name1", "admin code1", "admin name2", "admin code2", "admin name3", "admin code3",
	              "latitude", "longitude", "accuracy");

	private String csvFileName;
	private String[] headerNames;

	GeoDataFiles(String csvFileName, String... headerNames)
	{
		this.csvFileName = csvFileName;
	}

	public String getCsvFileName()
	{
		return csvFileName;
	}

	public String[] getHeaderNames()
	{
		return headerNames;
	}
}
