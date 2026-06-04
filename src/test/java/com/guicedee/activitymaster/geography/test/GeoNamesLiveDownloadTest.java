package com.guicedee.activitymaster.geography.test;

import geodata.GeoDataLocation;
import geodata.GeoNamesDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live test that downloads real GeoNames data from <a href="https://download.geonames.org/export/">download.geonames.org</a>.
 *
 * <p>Disabled by default and only enabled when the environment variable {@code GEONAMES_LIVE=true} is
 * set, so it never makes the regular build dependent on external network access. Andorra ({@code AD})
 * is used because its export files are tiny.</p>
 */
@EnabledIfEnvironmentVariable(named = "GEONAMES_LIVE", matches = "true")
public class GeoNamesLiveDownloadTest
{
	@TempDir
	Path homeDir;

	@Test
	public void downloadsRealCountryDataIntoHomeDirectory() throws IOException
	{
		Path original = GeoDataLocation.getBaseDirectory();
		GeoDataLocation.setBaseDirectory(homeDir);
		try
		{
			GeoNamesDownloader downloader = new GeoNamesDownloader(true);
			downloader.downloadCountry("AD");

			Path geoData = GeoDataLocation.countryGeoDataFile("AD");
			Path postal = GeoDataLocation.countryPostalCodeFile("AD");

			assertTrue(Files.exists(geoData) && Files.size(geoData) > 0, "AD geo-data should be downloaded");
			assertTrue(Files.exists(postal) && Files.size(postal) > 0, "AD postal codes should be downloaded");
		}
		finally
		{
			GeoDataLocation.setBaseDirectory(original);
		}
	}
}

