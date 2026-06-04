package geodata;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central configuration for where GeoNames export files are downloaded to and read from.
 *
 * <p>By default everything lives under the user's home directory in
 * {@code ~/.activitymaster/geonames}. The layout is:</p>
 * <pre>
 *   ~/.activitymaster/geonames/
 *       dump/          &lt;- export/dump files (countryInfo, admin1/2 codes, feature codes, time zones, {CC}.txt geo data, hierarchy)
 *       zip/           &lt;- export/zip files (per country {CC}.txt postal codes)
 * </pre>
 *
 * <p>The base directory can be overridden with the system property
 * {@code geonames.home} or the environment variable {@code GEONAMES_HOME}, or
 * programmatically via {@link #setBaseDirectory(Path)}.</p>
 */
public final class GeoDataLocation
{
	private static volatile Path baseDirectory = resolveDefaultBaseDirectory();

	private GeoDataLocation()
	{
	}

	private static Path resolveDefaultBaseDirectory()
	{
		String configured = System.getProperty("geonames.home", System.getenv("GEONAMES_HOME"));
		if (configured != null && !configured.isBlank())
		{
			return Paths.get(configured.trim());
		}
		return Paths.get(System.getProperty("user.home"), ".activitymaster", "geonames");
	}

	/**
	 * @return the configured base directory all GeoNames files are stored beneath.
	 */
	public static Path getBaseDirectory()
	{
		return baseDirectory;
	}

	/**
	 * Overrides the base directory (primarily for tests).
	 *
	 * @param directory the new base directory
	 */
	public static void setBaseDirectory(Path directory)
	{
		if (directory == null)
		{
			throw new IllegalArgumentException("GeoNames base directory may not be null");
		}
		baseDirectory = directory;
	}

	/**
	 * @return the directory holding the export/dump reference and geo-data files.
	 */
	public static Path dumpDirectory()
	{
		return baseDirectory.resolve("dump");
	}

	/**
	 * @return the directory holding the export/zip postal-code files.
	 */
	public static Path zipDirectory()
	{
		return baseDirectory.resolve("zip");
	}

	/**
	 * Resolves the downloaded per-country geo-data file ({@code {CC}.txt}) under the dump directory.
	 *
	 * @param countryCode the ISO-3166 alpha-2 country code
	 * @return the path to the geo-data text file for the country
	 */
	public static Path countryGeoDataFile(String countryCode)
	{
		return dumpDirectory().resolve(countryCode.toUpperCase() + ".txt");
	}

	/**
	 * Resolves the downloaded per-country postal-code file ({@code {CC}.txt}) under the zip directory.
	 *
	 * @param countryCode the ISO-3166 alpha-2 country code
	 * @return the path to the postal-code text file for the country
	 */
	public static Path countryPostalCodeFile(String countryCode)
	{
		return zipDirectory().resolve(countryCode.toUpperCase() + ".txt");
	}
}

