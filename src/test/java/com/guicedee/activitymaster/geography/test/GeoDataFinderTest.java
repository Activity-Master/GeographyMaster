package com.guicedee.activitymaster.geography.test;

import com.guicedee.activitymaster.geography.services.dto.GeographyCountry;
import geodata.GeoDataFinder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import static geodata.GeoDataFiles.CountryInfo;

/**
 * Relocated from the {@code geodata} package so the geography test sources can form their own JPMS
 * test module ({@code activity.master.geography.tests}) without a split-package clash against the
 * main module's {@code geodata} package.
 */
class GeoDataFinderTest
{
	@Test
	public void testGeoDataFileFinder()
	{
		try (GeoDataFinder finder = new GeoDataFinder(CountryInfo, CSVFormat.TDF, CountryInfo.getHeaderNames()))
		{
			for (CSVRecord record : finder.getRecords())
			{
				String code = record.get(0);
				String name = record.get(1);
				String nameAscii = record.get(2);
				String id = record.get(3);

				GeographyCountry country = new GeographyCountry();
				// country.set
			}
		}
	}
}

