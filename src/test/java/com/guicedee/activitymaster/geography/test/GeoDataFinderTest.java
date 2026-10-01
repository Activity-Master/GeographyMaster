package com.guicedee.activitymaster.geography.test;

import com.guicedee.activitymaster.geography.services.dto.GeographyCountry;
import geodata.GeoDataFinder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import static geodata.GeoDataFiles.CountryInfo;

/**
 * Relocated from the {@code geodata} package so the geography test sources can form their own JPMS
 * test module ({@code activity.master.geography.tests}) without a split-package clash against the
 * main module's {@code geodata} package.
 */
class GeoDataFinderTest
{
	@Test
	void quotedPlaceNamesRemainLiteral(@TempDir Path directory) throws Exception
	{
		Path feed = directory.resolve("GR.txt");
		Files.writeString(feed, "13526837\t\"Kostas Tsianos\" Theatre\t\"Kostas Tsianos\" Theatre\t\tGR\n"
				+ "13526840\tAlkazar Garden Theatre\tAlkazar Garden Theatre\t\tGR\n");
		try (GeoDataFinder finder = new GeoDataFinder(feed, CSVFormat.TDF, "id", "name", "ascii", "aliases", "country"))
		{
			var rows = finder.getRecords().iterator();
			var first = rows.next();
			assertEquals(5, first.size());
			assertEquals("\"Kostas Tsianos\" Theatre", first.get("name"));
			assertEquals("", first.get("aliases"));
			assertEquals("GR", first.get("country"));
			assertEquals("13526840", rows.next().get("id"));
			assertFalse(rows.hasNext());
		}
	}
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

