package geodata;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeoDataFinder
		implements AutoCloseable
{

	private Reader in;
	private Iterable<CSVRecord> records;
	private CSVParser parser;
	private InputStream is;

	/**
	 * Reads a tab-delimited GeoNames file directly from the filesystem (e.g. a file downloaded into
	 * the user's home directory by {@link GeoNamesDownloader}).
	 *
	 * @param file        the file to read
	 * @param format      the CSV format (delimiter is forced to tab)
	 * @param headerNames the column header names to apply
	 */
	public GeoDataFinder(Path file, CSVFormat format, String... headerNames)
	{
		try
		{
			is = Files.newInputStream(file);
			in = new InputStreamReader(is, StandardCharsets.UTF_8);
			parser = new CSVParser(in, format.withDelimiter('\t')
			                                 .withHeader(headerNames));
			records = parser.getRecords();
		}
		catch (IOException e)
		{
			throw new RuntimeException("Unable to read GeoNames file: " + file, e);
		}
	}

	public GeoDataFinder(GeoDataFiles file, CSVFormat format, String... headerNames)
	{
		String fileToRead = file.getCsvFileName();
		is = GeoDataFiles.class.getResourceAsStream(fileToRead);
		in = new InputStreamReader(is);
		try
		{
			parser = new CSVParser(in, format.withDelimiter('\t')
			                                 .withHeader(headerNames));
			records = parser.getRecords();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public GeoDataFinder(GeoDataFiles file, CSVFormat format)
	{
		String fileToRead = file.getCsvFileName();
		is = GeoDataFiles.class.getResourceAsStream(fileToRead);
		in = new InputStreamReader(is);
		try
		{
			parser = new CSVParser(in, format.withDelimiter('\t')
					.withFirstRecordAsHeader());
			records = parser.getRecords();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}


	public Iterable<CSVRecord> getRecords()
	{
		return records;
	}

	@Override
	public void close()
	{
		if (parser != null && !parser.isClosed())
		{
			try
			{
				parser.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		if (in != null)
		{
			try
			{
				in.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		if (is != null)
		{
			try
			{
				is.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
