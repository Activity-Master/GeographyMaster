package com.guicedee.activitymaster.geography.test;

import com.sun.net.httpserver.HttpServer;
import geodata.GeoDataLocation;
import geodata.GeoNamesDownloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deterministic, network-free test for {@link GeoNamesDownloader}. It serves a miniature GeoNames
 * export from a local {@link HttpServer} and asserts the downloader writes the reference files and the
 * extracted per-country geo-data and postal-code files into the configured home directory layout.
 */
public class GeoNamesDownloaderTest
{
	private HttpServer server;
	private String baseUrl;
	private Path originalBaseDir;

	@TempDir
	Path homeDir;

	@BeforeEach
	public void start() throws IOException
	{
		originalBaseDir = GeoDataLocation.getBaseDirectory();
		GeoDataLocation.setBaseDirectory(homeDir);

		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

		// Reference text files
		for (String file : GeoNamesDownloader.REFERENCE_TEXT_FILES)
		{
			serveBytes("/dump/" + file, ("# " + file + "\n").getBytes(StandardCharsets.UTF_8));
		}
		// Reference zips
		serveBytes("/dump/hierarchy.zip", zip("hierarchy.txt", "100\t200\tADM\n"));
		serveBytes("/dump/adminCode5.zip", zip("adminCode5.txt", "1234\tX5\n"));

		// Per-country geo-data and postal codes
		serveBytes("/dump/ZZ.zip", zip("ZZ.txt", "9999\tTestville\tTestville\t\t1.0\t2.0\tP\tPPL\tZZ\t\t01\t\t\t\t100\t\t10\tEtc/Test\t2020-01-01\n"));
		serveBytes("/zip/ZZ.zip", zip("ZZ.txt", "ZZ\t0001\tTestville\tProvince\t01\t\t\t\t\t1.0\t2.0\t4\n"));

		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	public void stop()
	{
		if (server != null)
		{
			server.stop(0);
		}
		GeoDataLocation.setBaseDirectory(originalBaseDir);
	}

	@Test
	public void downloadsReferenceAndCountryFilesIntoHomeDirectory() throws IOException
	{
		GeoNamesDownloader downloader = new GeoNamesDownloader(true, baseUrl + "/dump", baseUrl + "/zip");
		downloader.downloadReferenceData();
		downloader.downloadCountry("ZZ");

		// Reference text files landed in dump/
		for (String file : GeoNamesDownloader.REFERENCE_TEXT_FILES)
		{
			Path target = GeoDataLocation.dumpDirectory().resolve(file);
			assertTrue(Files.exists(target), () -> "Expected reference file " + target);
			assertTrue(Files.size(target) > 0, () -> "Reference file should not be empty: " + target);
		}

		// Reference zips were extracted to their inner txt entries
		assertTrue(Files.exists(GeoDataLocation.dumpDirectory().resolve("hierarchy.txt")), "hierarchy.txt extracted");
		assertTrue(Files.exists(GeoDataLocation.dumpDirectory().resolve("adminCode5.txt")), "adminCode5.txt extracted");

		// Per-country files extracted to the dump (geo-data) and zip (postal) directories
		Path geoData = GeoDataLocation.countryGeoDataFile("ZZ");
		Path postal = GeoDataLocation.countryPostalCodeFile("ZZ");
		assertTrue(Files.exists(geoData), () -> "Country geo-data extracted to " + geoData);
		assertTrue(Files.exists(postal), () -> "Country postal codes extracted to " + postal);

		String geoContent = Files.readString(geoData);
		assertTrue(geoContent.contains("Testville"), "Geo-data should contain the seeded place");
		String postalContent = Files.readString(postal);
		assertTrue(postalContent.contains("0001"), "Postal codes should contain the seeded code");

		// Re-download is a no-op when not forced and files already exist
		GeoNamesDownloader keep = new GeoNamesDownloader(false, baseUrl + "/dump", baseUrl + "/zip");
		long sizeBefore = Files.size(geoData);
		keep.downloadCountry("ZZ");
		assertEquals(sizeBefore, Files.size(geoData), "Existing file should be retained when not forced");
	}

	private void serveBytes(String path, byte[] body)
	{
		server.createContext(path, exchange -> {
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream os = exchange.getResponseBody())
			{
				os.write(body);
			}
		});
	}

	private static byte[] zip(String entryName, String content) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(bos))
		{
			zos.putNextEntry(new ZipEntry(entryName));
			zos.write(content.getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
		}
		return bos.toByteArray();
	}
}

