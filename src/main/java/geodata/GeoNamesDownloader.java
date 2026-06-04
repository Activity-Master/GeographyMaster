package geodata;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads GeoNames export files from <a href="https://download.geonames.org/export/">download.geonames.org</a>
 * into the user's home directory (see {@link GeoDataLocation}) so the geography loaders can ingest them
 * into ActivityMaster.
 *
 * <p>Two GeoNames data sets are supported:</p>
 * <ul>
 *     <li><b>export/dump</b> &ndash; reference data (country info, admin1/admin2 codes, feature codes,
 *     time zones, ISO language codes, hierarchy) and per-country geo-data ({@code {CC}.zip} &rarr; {@code {CC}.txt}).</li>
 *     <li><b>export/zip</b> &ndash; per-country postal codes ({@code {CC}.zip} &rarr; {@code {CC}.txt}).</li>
 * </ul>
 *
 * <p>All downloads are skipped when the target file already exists unless {@code force} is requested,
 * making repeated installs cheap and resumable.</p>
 */
@Log4j2
public class GeoNamesDownloader
{
	/** Base URL for the GeoNames dump (reference + geo-data) export. */
	public static final String DUMP_BASE_URL = "https://download.geonames.org/export/dump/";
	/** Base URL for the GeoNames postal-code (zip) export. */
	public static final String ZIP_BASE_URL = "https://download.geonames.org/export/zip/";

	/**
	 * Plain reference files served directly as {@code .txt} from the dump export. These apply to all
	 * countries and only need downloading once.
	 */
	public static final List<String> REFERENCE_TEXT_FILES = List.of(
		"countryInfo.txt",
		"admin1CodesASCII.txt",
		"admin2Codes.txt",
		"featureCodes_en.txt",
		"timeZones.txt",
		"iso-languagecodes.txt"
	);

	/**
	 * Zipped reference archives served from the dump export. Each entry maps the archive name to the
	 * single inner text file we want to retain.
	 */
	public static final List<String[]> REFERENCE_ZIP_FILES = List.of(
		new String[]{"hierarchy.zip", "hierarchy.txt"},
		new String[]{"adminCode5.zip", "adminCode5.txt"}
	);

	private final HttpClient httpClient;
	private final boolean force;
	private final String dumpBaseUrl;
	private final String zipBaseUrl;

	public GeoNamesDownloader()
	{
		this(false);
	}

	/**
	 * @param force when {@code true} existing files are re-downloaded; otherwise present files are kept.
	 */
	public GeoNamesDownloader(boolean force)
	{
		this(force, DUMP_BASE_URL, ZIP_BASE_URL);
	}

	/**
	 * @param force       when {@code true} existing files are re-downloaded; otherwise present files are kept.
	 * @param dumpBaseUrl base URL for the dump export (overridable for testing)
	 * @param zipBaseUrl  base URL for the postal-code export (overridable for testing)
	 */
	public GeoNamesDownloader(boolean force, String dumpBaseUrl, String zipBaseUrl)
	{
		this.force = force;
		this.dumpBaseUrl = dumpBaseUrl.endsWith("/") ? dumpBaseUrl : dumpBaseUrl + "/";
		this.zipBaseUrl = zipBaseUrl.endsWith("/") ? zipBaseUrl : zipBaseUrl + "/";
		this.httpClient = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(30))
			.build();
	}

	/**
	 * Downloads the global reference data set (country info, admin codes, feature codes, time zones,
	 * ISO language codes, hierarchy and adminCode5) into {@link GeoDataLocation#dumpDirectory()}.
	 *
	 * @throws IOException if a download or extraction fails
	 */
	public void downloadReferenceData() throws IOException
	{
		Path dumpDir = GeoDataLocation.dumpDirectory();
		Files.createDirectories(dumpDir);
		log.info("Downloading GeoNames reference data into {}", dumpDir.toAbsolutePath());

		for (String fileName : REFERENCE_TEXT_FILES)
		{
			downloadFile(URI.create(dumpBaseUrl + fileName), dumpDir.resolve(fileName));
		}
		for (String[] archive : REFERENCE_ZIP_FILES)
		{
			downloadAndExtract(URI.create(dumpBaseUrl + archive[0]), dumpDir, archive[1], dumpDir.resolve(archive[1]));
		}
		log.info("GeoNames reference data ready in {}", dumpDir.toAbsolutePath());
	}

	/**
	 * Downloads the per-country geo-data archive ({@code dump/{CC}.zip}) and postal-code archive
	 * ({@code zip/{CC}.zip}), extracting the inner {@code {CC}.txt} files into the dump and zip
	 * directories respectively.
	 *
	 * @param countryCode the ISO-3166 alpha-2 country code (case-insensitive)
	 * @throws IOException if a download or extraction fails
	 */
	public void downloadCountry(String countryCode) throws IOException
	{
		String cc = countryCode.toUpperCase();
		Path dumpDir = GeoDataLocation.dumpDirectory();
		Path zipDir = GeoDataLocation.zipDirectory();
		Files.createDirectories(dumpDir);
		Files.createDirectories(zipDir);

		log.info("Downloading GeoNames geo-data + postal codes for country {}", cc);
		// Per-country geo-data (towns, cities, places)
		downloadAndExtract(URI.create(dumpBaseUrl + cc + ".zip"), dumpDir, cc + ".txt", dumpDir.resolve(cc + ".txt"));
		// Per-country postal codes
		downloadAndExtract(URI.create(zipBaseUrl + cc + ".zip"), zipDir, cc + ".txt", zipDir.resolve(cc + ".txt"));
		log.info("GeoNames data for country {} ready", cc);
	}

	/**
	 * Convenience helper that downloads the reference data set and the requested countries in one pass.
	 *
	 * @param countryCodes the ISO-3166 alpha-2 country codes to download
	 * @throws IOException if a download or extraction fails
	 */
	public void downloadAll(List<String> countryCodes) throws IOException
	{
		downloadReferenceData();
		List<String> failures = new ArrayList<>();
		for (String countryCode : countryCodes)
		{
			try
			{
				downloadCountry(countryCode);
			}
			catch (IOException e)
			{
				log.error("Failed to download GeoNames data for {}: {}", countryCode, e.getMessage(), e);
				failures.add(countryCode);
			}
		}
		if (!failures.isEmpty())
		{
			log.warn("Completed GeoNames download with {} failed countr(y/ies): {}", failures.size(), failures);
		}
	}

	/**
	 * Downloads a single file unless it already exists (and {@code force} is disabled).
	 *
	 * @param uri    the remote file
	 * @param target the local destination
	 * @throws IOException if the download fails
	 */
	public void downloadFile(URI uri, Path target) throws IOException
	{
		if (!force && Files.exists(target) && fileSize(target) > 0)
		{
			log.debug("Skipping existing file {}", target.getFileName());
			return;
		}
		Files.createDirectories(target.getParent());
		log.info("Downloading {} -> {}", uri, target.toAbsolutePath());
		try
		{
			HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofMinutes(10))
				.header("User-Agent", "ActivityMaster-Geography/1.0")
				.GET()
				.build();
			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200)
			{
				try (InputStream ignored = response.body())
				{
					throw new IOException("Unexpected HTTP status " + response.statusCode() + " for " + uri);
				}
			}
			try (InputStream body = response.body())
			{
				Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
			}
			log.info("Downloaded {} ({} bytes)", target.getFileName(), fileSize(target));
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Download interrupted for " + uri, e);
		}
	}

	/**
	 * Downloads a ZIP archive and extracts the named inner entry to the given destination.
	 *
	 * @param uri        the remote zip archive
	 * @param workingDir directory used for the temporary archive
	 * @param entryName  the inner entry to keep (e.g. {@code ZA.txt})
	 * @param target     the destination path for the extracted entry
	 * @throws IOException if the download or extraction fails
	 */
	public void downloadAndExtract(URI uri, Path workingDir, String entryName, Path target) throws IOException
	{
		if (!force && Files.exists(target) && fileSize(target) > 0)
		{
			log.debug("Skipping existing extracted file {}", target.getFileName());
			return;
		}
		Files.createDirectories(workingDir);
		Path tempZip = Files.createTempFile(workingDir, "geonames-", ".zip");
		try
		{
			downloadFileForced(uri, tempZip);
			extractEntry(tempZip, entryName, target);
		}
		finally
		{
			Files.deleteIfExists(tempZip);
		}
	}

	private void downloadFileForced(URI uri, Path target) throws IOException
	{
		Files.createDirectories(target.getParent());
		log.info("Downloading archive {} -> {}", uri, target.getFileName());
		try
		{
			HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofMinutes(15))
				.header("User-Agent", "ActivityMaster-Geography/1.0")
				.GET()
				.build();
			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200)
			{
				try (InputStream ignored = response.body())
				{
					throw new IOException("Unexpected HTTP status " + response.statusCode() + " for " + uri);
				}
			}
			try (InputStream body = response.body())
			{
				Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Download interrupted for " + uri, e);
		}
	}

	private void extractEntry(Path zipFile, String entryName, Path target) throws IOException
	{
		Files.createDirectories(target.getParent());
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile)))
		{
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null)
			{
				if (entry.isDirectory())
				{
					continue;
				}
				if (entry.getName().equalsIgnoreCase(entryName))
				{
					Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
					log.info("Extracted {} -> {} ({} bytes)", entryName, target.toAbsolutePath(), fileSize(target));
					return;
				}
			}
		}
		throw new IOException("Entry " + entryName + " not found inside " + zipFile.getFileName());
	}

	private static long fileSize(Path path)
	{
		try
		{
			return Files.size(path);
		}
		catch (IOException e)
		{
			return -1;
		}
	}
}

