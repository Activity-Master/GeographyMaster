package geodata;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class GeoDataFinder
		implements AutoCloseable
{

	private Reader in;
	private Iterable<CSVRecord> records;
	private CSVParser parser;
	private InputStream is;

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
