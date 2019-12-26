package geodata;

import com.guicedee.activitymaster.geography.services.dto.GeographyCountry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import static geodata.GeoDataFiles.*;
import static org.junit.jupiter.api.Assertions.*;

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
				//country.set
			}
		}
	}
}
