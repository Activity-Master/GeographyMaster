package com.guicedee.activitymaster.geography;

import com.google.common.base.Strings;
import com.google.inject.Singleton;
import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.enterprise.Enterprise;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.implementations.ClassificationService;
import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.IProgressable;
import com.guicedee.activitymaster.core.services.classifications.enterprise.IEnterpriseName;
import com.guicedee.activitymaster.core.services.dto.IClassification;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.system.IClassificationDataConceptService;
import com.guicedee.activitymaster.core.services.system.IClassificationService;
import com.guicedee.activitymaster.core.services.system.IEnterpriseService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.*;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.logger.LogFactory;
import geodata.GeoDataFinder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;
import static com.guicedee.guicedinjection.json.StaticStrings.*;
import static geodata.GeoDataFiles.*;

@Singleton
public class GeographyService<J extends GeographyService<J>>
		implements IProgressable,
		           IGeographyService<J>
{
	public IGeography<?> createPlanet(@CacheKey @NotNull String value, String originalUniqueID, IEnterprise<?> enterprise, UUID... identityToken)
	{
		PlanetService service = get(PlanetService.class);
		return service.createPlanet(value, "The planet " + value, originalUniqueID, enterprise, identityToken);
	}
	
	public IGeography<?> createContinent(String planetName, GeographyContinent continent, ISystems<?> originatingSystem, String originalUniqueID, UUID... identityToken)
	{
		PlanetService planetService = get(PlanetService.class);
		IGeography<?> planet = planetService.findPlanet(planetName, originatingSystem.getEnterprise(), identityToken);
		ContinentService service = get(ContinentService.class);
		return service.createContinent(planet, continent.getContinentCode(), continent.getContinentName(), originalUniqueID, originatingSystem.getEnterprise(), identityToken);
	}
	
	
	@Override
	@CacheResult
	public IGeography<?> findPlanet(@CacheKey String name, @CacheKey ISystems<?> originatingSystem, @CacheKey UUID... identityToken)
	{
		PlanetService service = get(PlanetService.class);
		return service.findPlanet(name, originatingSystem.getEnterprise(), identityToken);
	}
	
	
	@Override
	@CacheResult
	public GeographyContinent findContinent(@CacheKey GeographyContinent continent, @CacheKey ISystems<?> originatingSystem, @CacheKey UUID... identityToken)
	{
		ContinentService service = get(ContinentService.class);
		IGeography<?> continentGeo = service.findContinent(continent.getContinentCode(), originatingSystem.getEnterprise(), identityToken);
		GeographyContinent gc = new GeographyContinent();
		gc.setContinentCode(continentGeo.getName());
		gc.setContinentName(continentGeo.getDescription());
		gc.setGeographyId(continentGeo.getId());
		return gc;
	}
	
	@Override
	@CacheResult
	public GeographyCountry findCountry(@CacheKey GeographyCountry country, @CacheKey IEnterpriseName<?> enterpriseName, @CacheKey UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		CountryService cs = get(CountryService.class);
		IGeography<?> geo = cs.findCountry(country.getIso(), enterprise, identityToken);
		GeographyCountry gc = new GeographyCountry();
		
		List<Object[]> values = geo.getValues(CountryISO3166, null, geoSystem, identityToken, CountryISO3166_3, CountryISO_Numeric,
		                                      CountryFips, CountryCapital, CountryAreaInSqKm, CountryTld, CountryPhone, CountryPostalCodeFormat, CountryPostalCodeRegex);
		if (!values.isEmpty())
		{
			Object[] vals = values.stream()
			                      .findFirst()
			                      .get();
			gc.setIso(vals[0].toString());
			gc.setIso3(vals[1].toString());
			gc.setIsoNumeric(vals[2].toString());
			gc.setFips(vals[3].toString());
			gc.setCapital(vals[4].toString());
			gc.setAreaSqlKM(vals[5].toString());
			gc.setWebTld(vals[6].toString());
			gc.setCountryDialCode(vals[7].toString());
			gc.setPostalCodeDecimalFormat(vals[8].toString());
			gc.setPostalCodeRegexFormat(vals[9].toString());
		}
		
		if (geo.hasClassifications(Currency, geoSystem, identityToken))
		{
			String currency = geo.findClassifications(Currency, enterprise, identityToken)
			                     .orElseThrow()
			                     .getValue();
			CurrencyService currencyService = get(CurrencyService.class);
			IClassification<?> geoCurrency = currencyService.findCurrency(currency, enterprise, identityToken);
			GeographyCurrency geographyCurrency = new GeographyCurrency();
			geographyCurrency.setCurrencyCode(geoCurrency.getName());
			geographyCurrency.setCurrencyName(geoCurrency.getDescription());
			gc.setCurrency(geographyCurrency);
		}
		
		gc.setGeographyId(geo.getId());
		if (!geo.getOriginalSourceSystemUniqueID()
		        .isEmpty())
		{
			gc.setGeonameId(Long.parseLong(geo.getOriginalSourceSystemUniqueID()));
		}
		
		return gc;
	}
	
	public GeographyCountry createCountry(GeographyCountry country, IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		CountryService countryService = get(CountryService.class);
		IGeography<?> geoContinent = get(ContinentService.class).findContinent(country.getContinent()
		                                                                              .getContinentCode(), enterprise, identityToken);
		IGeography<?> geoCountry = countryService.createCountry(geoContinent, country.getIso(), country.getCountryName(), country.getGeonameId() + "", enterprise, identityToken);
		
		
		IClassification<?> currency = get(ClassificationService.class).find(country.getCurrency()
		                                                                           .getCurrencyCode(), GeographyCurrencyConcept, enterprise, identityToken);
		geoCountry.addOrUpdate(Currency, currency.getName(), geoSystem, identityToken);
		
		countryService.updateCountry(currency, country.getIso(), country.getCountryName(), country.getIso3(), country.getIsoNumeric(), country.getCountryDialCode(),
		                             country.getFips(), country.getCapital(), country.getAreaSqlKM(), country.getPostalCodeDecimalFormat(), country.getPostalCodeRegexFormat(),
		                             country.getPopulation(), country.getWebTld(), enterprise, identityToken);
		
		country.setGeographyId(geoCountry.getId());
		return country;
	}
	
	@Override
	public void loadProvincesASCII1(IEnterpriseName<?> enterpriseName, String countryCode, IActivityMasterProgressMonitor progressMonitor)
	{
		progressMonitor.setTotalTasks(4100);
		try (GeoDataFinder finder = new GeoDataFinder(Admin1CodesASCII, CSVFormat.TDF, Admin1CodesASCII.getHeaderNames()))
		{
			int current = 0;
			for (CSVRecord record : finder.getRecords())
			{
				current++;
				GeographyAsciiCode ascii = new GeographyAsciiCode();
				ascii.setCode(record.get(0))
				     .setName(record.get(1))
				     .setNameAscii(record.get(2))
				     .setGeonameId(Long.parseLong(record.get(3)));
				
				if (countryCode != null && ascii.getCode()
				                                .startsWith(countryCode.toUpperCase() + "."))
				{
					ProvinceService provinceService = get(ProvinceService.class);
					CountryService countryService = get(CountryService.class);
					IGeography<?> country = countryService.findCountry(countryCode, enterpriseName.getEnterprise());
					IGeography<?> province = provinceService.createProvince(country, ascii.getCode(), ascii.getName(), ascii.getGeonameId() + "", enterpriseName.getEnterprise());
				}
				if (current % 10 == 0)
				{
					logProgress("Geography Service", "Loading Province Codes", 10, progressMonitor);
				}
			}
			logProgress("Geography Service", "Finished Province Codes", 0, progressMonitor);
		}
	}
	
	@Override
	public void loadDistrictsASCII2(IEnterpriseName<?> enterpriseName, String countryCode, IActivityMasterProgressMonitor progressMonitor)
	{
		progressMonitor.setTotalTasks(4500);
		try (GeoDataFinder finder = new GeoDataFinder(Admin2Codes, CSVFormat.TDF, Admin2Codes.getHeaderNames()))
		{
			int current = 0;
			for (CSVRecord record : finder.getRecords())
			{
				current++;
				GeographyAsciiCode ascii = new GeographyAsciiCode();
				ascii.setCode(record.get(0))
				     .setName(record.get(1))
				     .setNameAscii(record.get(2))
				     .setGeonameId(Long.parseLong(record.get(3)));
				
				if (countryCode != null && ascii.getCode()
				                                .startsWith(countryCode.toUpperCase() + "."))
				{
					String provinceCode = ascii.getCode()
					                           .substring(0, 5);
					
					ProvinceService provinceService = get(ProvinceService.class);
					IGeography<?> province = provinceService.findProvince(provinceCode, enterpriseName.getEnterprise());
					DistrictService districtService = get(DistrictService.class);
					districtService.createDistrict(province, ascii.getCode(), ascii.getName(), ascii.getGeonameId() + "", enterpriseName.getEnterprise());
				}
				if (current % 10 == 0)
				{
					logProgress("Geography Service", "Loading Districts/Cities", 10, progressMonitor);
				}
			}
		}
		logProgress("Geography Service", "Finished Districts/Cities", 10, progressMonitor);
	}
	
	@Override
	public void loadLanguages(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{
		progressMonitor.setTotalTasks(547);
		try (GeoDataFinder finder = new GeoDataFinder(ISO639Languages, CSVFormat.TDF, ISO639Languages.getHeaderNames()))
		{
			int current = 0;
			logProgress("Geography Service", "Starting Geography Associated Languages", 1, progressMonitor);
			for (CSVRecord record : finder.getRecords())
			{
				current++;
				ISO639Language language = new ISO639Language();
				String code2 = record.get(0);
				String code1 = record.get(1);
				String english = record.get(2);
				String french = record.get(3);
				String german = record.get(4);
				
				language.setIso6391Code(code1);
				language.setIso6392Code(code2);
				if (!english.isEmpty())
				{
					StringTokenizer st = new StringTokenizer(english, ";");
					while (st.hasMoreTokens())
					{
						String s = st.nextToken();
						language.getName()
						        .add(s);
					}
				}
				if (!french.isEmpty())
				{
					StringTokenizer st = new StringTokenizer(french, ";");
					while (st.hasMoreTokens())
					{
						String s = st.nextToken();
						language.getFrenchName()
						        .add(s);
					}
				}
				if (!german.isEmpty())
				{
					StringTokenizer st = new StringTokenizer(german, ";");
					while (st.hasMoreTokens())
					{
						String s = st.nextToken();
						language.getGermanName()
						        .add(s);
					}
				}
				
				LanguagesService languagesService = get(LanguagesService.class);
				IClassification<?> lang = languagesService.createLanguage(language.getIso6391Code(), english, language.getIso6391Code(), enterpriseName.getEnterprise());
				languagesService.updateLanguage(lang.getName(), null, language.getIso6392Code(), null, null, null, enterpriseName.getEnterprise());
				
				for (String s : language.getName())
				{
					languagesService.updateLanguage(lang.getName(), null, null, s, null, null, enterpriseName.getEnterprise());
				}
				for (String s : language.getFrenchName())
				{
					languagesService.updateLanguage(lang.getName(), null, null, null, s, null, enterpriseName.getEnterprise());
				}
				for (String s : language.getGermanName())
				{
					languagesService.updateLanguage(lang.getName(), null, null, null, null, s, enterpriseName.getEnterprise());
				}
				
				if (current % 5 == 0)
				{
					logProgress("Geography Service", "Loading Language - " +
							            (language.getName()
							                     .isEmpty() ? " - " : language.getName()
							                                                  .toArray()[0])
							, 5, progressMonitor);
				}
			}
		}
		logProgress("Geography Service", "Geography Associated Languages queued", 1, progressMonitor);
	}
	
	
	@Override
	public void loadCountryInfo(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		
		progressMonitor.setTotalTasks(252);
		try (GeoDataFinder finder = new GeoDataFinder(CountryInfo, CSVFormat.TDF, CountryInfo.getHeaderNames()))
		{
			int current = 0;
			for (CSVRecord record : finder.getRecords())
			{
				current++;
				GeographyCountry country = new GeographyCountry();
				country.setIso(record.get(0));
				country.setIso3(record.get(1));
				country.setIsoNumeric(record.get(2));
				country.setFips(record.get(3));
				country.setCountryName(record.get(4));
				country.setCapital(record.get(5));
				country.setAreaSqlKM(record.get(6));
				try
				{
					country.setPopulation(Integer.parseInt(record.get(7)));
				}
				catch (NumberFormatException nfe)
				{
					country.setPopulation(0);
				}
				
				String continentCode = record.get(8);
				GeographyContinent gc = findContinent(new GeographyContinent().setContinentCode(continentCode), geoSystem, identityToken);
				country.setContinent(gc);
				
				country.setWebTld(record.get(9));
				
				IClassification<?> currencyClassification = get(CurrencyService.class).createCurrency(record.get(10), record.get(11), enterprise, identityToken);
				GeographyCurrency gcc = new GeographyCurrency().setCurrencyCode(currencyClassification.getName())
				                                               .setCurrencyName(currencyClassification.getDescription());
				country.setCurrency(gcc);
				
				country.setCountryDialCode(record.get(12));
				country.setPostalCodeDecimalFormat(record.get(13));
				country.setPostalCodeRegexFormat(record.get(14));
				
				String languagesList = record.get(15);
				for (String s : languagesList.split(","))
				{
					ISO639Language lang = new ISO639Language();
					lang.setIso6391Code(s);
					country.getLanguages()
					       .add(lang);
				}
				
				try
				{
					country.setGeonameId(Long.parseLong(record.get(16)));
				}
				catch (NumberFormatException nfe)
				{
				
				}
				if (record.size() > 17)
				{
					String neighbours = record.get(17);
				}
				if (record.size() > 18)
				{
					country.setEquivalentFips(record.get(18));
				}
				
				GeographyCountry gccc = createCountry(country, enterpriseName);
				logProgress("Geography Service", "Loaded Country " + country.getCountryName(), 1, progressMonitor);
			}
		}
		logProgress("Geography Service", "Finished Loading Countries", 10, progressMonitor);
	}
	
	/**
	 * By timezone ID
	 * <p>
	 * like Asia/Baghdad
	 *
	 * @param timezone
	 * @param enterpriseName
	 * @return
	 */
	@Override
	@CacheResult(cacheName = "GeographyTimezones")
	public GeographyTimezone findTimezone(@CacheKey GeographyTimezone timezone, @CacheKey IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		TimeZoneService timeZoneService = get(TimeZoneService.class);
		
		IClassification<?> timeZoneClassification = timeZoneService.findTimeZone(timezone.getTimezoneID(), enterprise, identityToken);
		GeographyTimezone tz = new GeographyTimezone();
		tz.setTimezoneID(timeZoneClassification.getName());
		List<Object[]> values = timeZoneClassification.getValues(TimeZoneRawOffset, null, geoSystem, new UUID[]{identityToken}, TimeZoneOffsetJuly2016, TimeZoneOffsetJan2016);
		if (!values.isEmpty())
		{
			if (values.get(0)[0] != null)
			{ tz.setRawOffset(Double.parseDouble(values.get(0)[0].toString())); }
			if (values.get(0)[1] != null)
			{ tz.setOffsetJuly2016(Double.parseDouble(values.get(0)[1].toString())); }
			if (values.get(0)[2] != null)
			{ tz.setOffsetJan2016(Double.parseDouble(values.get(0)[2].toString())); }
		}
		
		return tz;
	}
	
	@Override
	public void loadTimeZones(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{
		progressMonitor.setTotalTasks(425);
		try (GeoDataFinder finder = new GeoDataFinder(TimeZones, CSVFormat.TDF, TimeZones.getHeaderNames()))
		{
			int current = 0;
			for (CSVRecord record : finder.getRecords())
			{
				current++;
				GeographyTimezone timezone = new GeographyTimezone();
				timezone.setTimezoneID(record.get(1));
				timezone.setOffsetJan2016(Double.parseDouble(record.get(2)));
				timezone.setOffsetJuly2016(Double.parseDouble(record.get(3)));
				timezone.setRawOffset(Double.parseDouble(record.get(4)));
				create(timezone, enterpriseName);
				
				ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterpriseName.getEnterprise());
				UUID identityToken = get(GeographySystem.class).getSystemToken(enterpriseName.getEnterprise());
				CountryService cs = get(CountryService.class);
				IGeography<?> country = cs.findCountry(record.get(0), enterpriseName.getEnterprise());
				
				TimeZoneService timeZoneService = get(TimeZoneService.class);
				IClassification<?> timeZone = timeZoneService.findTimeZone(timezone.getTimezoneID(), enterpriseName.getEnterprise(), identityToken);
				country.addOrUpdate(TimeZone, timeZone.getName(), geoSystem, identityToken);
				
				if (current % 5 == 0)
				{ logProgress("TimeZones", "Loaded Timezone - " + timezone.getTimezoneID(), 5, progressMonitor); }
			}
		}
	}
	
	public GeographyTimezone create(GeographyTimezone timezone, IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		TimeZoneService timeZoneService = get(TimeZoneService.class);
		timeZoneService.createTimeZone(timezone.getTimezoneID(), timezone.getTimezoneID(), null, enterprise, identityToken);
		
		timeZoneService.updateTimeZone(timezone.getTimezoneID(), null,
		                               timezone.getRawOffset() + "", timezone.getOffsetJuly2016() + "", timezone.getOffsetJan2016() + "",
		                               enterprise, identityToken);
		return timezone;
	}
	
	@Override
	public void loadPostalCodes(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		//Postal Codes Full Listing
		Map<Long, List<GeographyPostalCode>> postalCodeMap = new HashMap<>();
		progressMonitor.setCurrentTask(0);
		progressMonitor.setTotalTasks(3921);
		try (GeoDataFinder finder = new GeoDataFinder(ZAPostalCodes, CSVFormat.TDF, ZAPostalCodes.getHeaderNames()))
		{
			int current = 0;
			for (CSVRecord record : finder.getRecords())
			{
				current++;
				GeographyPostalCode post = new GeographyPostalCode();
				String countryCode = record.get(0);
				GeographyCountry cunt = findCountry(new GeographyCountry().setIso(countryCode), enterpriseName, identityToken);
				post.setCountryCode(cunt);
				post.setPostalCode(record.get(1));
				post.setPostalCodePlaceName(record.get(2));
				post.setParentPlaceName(record.get(2));
				GeographyCoordinates coordinates = new GeographyCoordinates(record.get(9), record.get(10));
				post.setCoordinates(coordinates);
				
				if (!postalCodeMap.containsKey(Long.valueOf(post.getPostalCode())))
				{
					postalCodeMap.put(Long.valueOf(post.getPostalCode()), new ArrayList<>());
				}
				if (record.size() > 11)
				{
					String adminCodeType = record.get(11);
					if (!Strings.isNullOrEmpty(adminCodeType))
					{
						postalCodeMap.get(Long.valueOf(post.getPostalCode()))
						             .add(post);
					}
				}
				//create(post, enterpriseName);
				if (current % 3 == 0)
				{ logProgress("Postal Codes", "Loaded PostalCode - " + post.getPostalCode(), 3, progressMonitor); }
			}
		}
		
		progressMonitor.setCurrentTask(0);
		progressMonitor.setTotalTasks(19888);
		try (GeoDataFinder finder = new GeoDataFinder(ZAPostalCodesUpdates, CSVFormat.TDF))
		{
			int current = 0;
			Set<String> completedPostalCodes = new HashSet<>();
			for (CSVRecord record : finder.getRecords())
			{
				current++;
				GeographyPostalCode post = new GeographyPostalCode();
				String countryCode = record.get("countryCode");
				if (countryCode.equals("RSA")) //iso3
				{
					countryCode = "ZA"; //iso2
				}
				
				GeographyCountry country = findCountry(new GeographyCountry().setIso(countryCode), enterpriseName, identityToken);
				post.setCountryCode(country);
				
				Long pcFor = Long.parseLong(record.get("postalCode"));
				//String value = postalCodeFormat.format(pcFor);
				String value = String.valueOf(pcFor);
				
				post.setGeonameId(Long.valueOf(record.get("id")));
				
				post.setPostalCode(value);
				post.setPostalCodePlaceName(WordUtils.capitalize(StringUtils.lowerCase(record.get("suburb"))));
				post.setParentPlaceName(WordUtils.capitalize(StringUtils.lowerCase(record.get("city"))));
				
				String province = record.get("stateProvinceName");
				if (province.equalsIgnoreCase("Free State"))
				{ province = "Orange Free State"; }
				else if (province.equalsIgnoreCase("KwaZulu Natal"))
				{ province = "KwaZulu-Natal"; }
				else if (province.equalsIgnoreCase("North West Province"))
				{ province = "North-West"; }
				
				
				post.setProvinceName(province);
				
				if (!postalCodeMap.containsKey(Long.valueOf(post.getPostalCode())))
				{
					postalCodeMap.put(Long.valueOf(post.getPostalCode()), new ArrayList<>());
				}
				postalCodeMap.get(Long.valueOf(post.getPostalCode()))
				             .add(post);
				
				if (current % 3 == 0)
				{ logProgress("Postal Codes", "Loaded PostalCode Updates - " + post.getPostalCode(), 3, progressMonitor); }
			}
		}
		
		DistrictService districtService = get(DistrictService.class);
		PostalCodeService postalCodeService = get(PostalCodeService.class);
		int current = 0;
		progressMonitor.setTotalTasks(postalCodeMap.size());
		progressMonitor.setCurrentTask(0);
		for (Map.Entry<Long, List<GeographyPostalCode>> entry : postalCodeMap.entrySet())
		{
			Long key = entry.getKey();
			List<GeographyPostalCode> value = entry.getValue();
			current++;
			//Find the town, or the city/district
			TownService townService = get(TownService.class);
			GeographyPostalCode gp = value.get(0);
			if (value.size() > 1)
			{
				gp.setProvinceName(value.get(1)
				                        .getProvinceName());
			}
			IGeography<?> town = null;
			try
			{
				town = townService.findTown(gp.getParentPlaceName(), enterprise, identityToken);
			}
			catch (Throwable T)
			{
				if (!Strings.isNullOrEmpty(gp.getProvinceName()))
				{
					try
					{
						IGeography<?> firstDistrictInProvince = districtService.findFirstDistrictInProvince(gp.getProvinceName(), enterprise, identityToken);
						town = townService.createTown(firstDistrictInProvince,
						                              gp.getParentPlaceName(), gp.getParentPlaceName(),
						                              gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()),
						                              enterprise, identityToken);
						if (gp.getCoordinates() != null)
						{
							townService.updateTown(firstDistrictInProvince.getName(),
							                       town.getName(),
							                       null,
							                       gp.getCoordinates()
							                         .getLatitude(),
							                       gp.getCoordinates()
							                         .getLongitude(),
							                       null,
							                       null,
							                       null,
							                       null,
							                       null,
							                       enterprise,
							                       identityToken);
						}
					}
					catch (Exception e)
					{
						LogFactory.getLog(GeographyService.class)
						          .log(Level.SEVERE, "Whhops? " + gp, e);
					}
				}
				else
				{
					System.out.println("Now What? - " + gp);
				}
			}
			
			if (town != null)
			{
				IGeography<?> postalCode = postalCodeService.createPostalCode(town,
				                                                              gp.getPostalCode(), gp.getPostalCodePlaceName(),
				                                                              gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()),
				                                                              enterprise, identityToken);
				for (GeographyPostalCode geographyPostalCode : value)
				{
					IGeography<?> postalCodeSuburb = postalCodeService.createPostalCodeSuburb(postalCode, geographyPostalCode.getPostalCode(),
					                                                                          geographyPostalCode.getPostalCodePlaceName(),
					                                                                          geographyPostalCode.getPostalCode(), enterprise, identityToken
					);
					if (gp.getCoordinates() != null)
					{
						postalCodeService.updatePostalCode(null,
						                                   null,
						                                   postalCodeSuburb.getName(),
						                                   null,
						                                   gp.getCoordinates()
						                                     .getLatitude(),
						                                   gp.getCoordinates()
						                                     .getLongitude(),
						                                   enterprise,
						                                   identityToken);
					}
				}
				if (gp.getCoordinates() != null)
				{
					postalCodeService.updatePostalCodeParent(
							postalCode.getName(),
							null,
							gp.getCoordinates()
							  .getLatitude(),
							gp.getCoordinates()
							  .getLongitude(),
							enterprise,
							identityToken);
				}
			}
			if (current % 10 == 0)
			{ logProgress("Postal Codes", "Loaded District->PostalCode Updates - " + gp.getPostalCode(), 10, progressMonitor); }
		}
		//postalCodeService.createPostalCode();
	}
	
	@Override
	@CacheResult(cacheName = "GeographyPostalCodes")
	public GeographyPostalCode findPostalCode(@CacheKey GeographyPostalCode postalCode, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		PostalCodeService postalCodeService = get(PostalCodeService.class);
		IGeography<?> geo = postalCodeService.findPostalCode(null, postalCode.getPostalCode(), enterprise, identityToken);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		
		GeographyPostalCode result = new GeographyPostalCode();
		result.setGeographyId(geo.getId());
		result.setPostalCodePlaceName(geo.getDescription());
		if (geo.hasClassifications(Latitude, geoSystem, identityToken))
		{
			String latitude = geo.findClassifications(Latitude, enterprise, identityToken)
			                     .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                     .getValue();
			String longitude = geo.findClassifications(Longitude, enterprise, identityToken)
			                      .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                      .getValue();
			result.setCoordinates(new GeographyCoordinates(latitude, longitude));
		}
		return result;
	}
	
	
	@Override
	@CacheResult(cacheName = "GeographyPostalCodesSuburb")
	public GeographyPostalCode findPostalCodeSuburb(@CacheKey String code, @CacheKey String description, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		PostalCodeService postalCodeService = get(PostalCodeService.class);
		IGeography<?> geo = postalCodeService.findPostalCodeSuburb(code, description, enterprise, identityToken);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		
		GeographyPostalCode result = new GeographyPostalCode();
		result.setGeographyId(geo.getId());
		result.setPostalCodePlaceName(geo.getDescription());
		if (geo.hasClassifications(Latitude, geoSystem, identityToken))
		{
			String latitude = geo.findClassifications(Latitude, enterprise, identityToken)
			                     .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                     .getValue();
			String longitude = geo.findClassifications(Longitude, enterprise, identityToken)
			                      .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                      .getValue();
			result.setCoordinates(new GeographyCoordinates(latitude, longitude));
		}
		return result;
	}
	
	@Override
	@CacheResult(cacheName = "GeographyPostalCodesSuburb")
	public GeographyPostalCode findOrCreatePostalCodeSuburb(@CacheKey String code, @CacheKey String description, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		PostalCodeService postalCodeService = get(PostalCodeService.class);
		IGeography<?> geo = postalCodeService.findOrCreatePostalCodeSuburb(code, description, enterprise, identityToken);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		GeographyPostalCode result = new GeographyPostalCode();
		result.setGeographyId(geo.getId());
		result.setPostalCodePlaceName(geo.getDescription());
		if (geo.hasClassifications(Latitude, geoSystem, identityToken))
		{
			String latitude = geo.findClassifications(Latitude, enterprise, identityToken)
			                     .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                     .getValue();
			String longitude = geo.findClassifications(Longitude, enterprise, identityToken)
			                      .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                      .getValue();
			result.setCoordinates(new GeographyCoordinates(latitude, longitude));
		}
		return result;
	}
	
	@Override
	@CacheResult(cacheName = "GeographyFindGeographyById")
	public IGeography<?> findGeographyById(@CacheKey Long geographyID, @CacheKey IEnterpriseName<?> enterpriseName, @CacheKey UUID... identityToken)
	{
		return new Geography().builder()
		                      .find(geographyID)
		                      .inActiveRange(enterpriseName.getEnterprise(), identityToken)
		                      .inDateRange()
		                      .withEnterprise(enterpriseName.getEnterprise())
		                      .get(true)
		                      .orElse(null);
	}
	
	
	@CacheResult(cacheName = "GeographyFindFeatureClass")
	public IClassification<?> findFeatureClass(@CacheKey GeographyFeatureClassesClassifications key, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		return classificationService.find(key.classificationName(), enterprise, identityToken);
	}
	
	/**
	 * Created with everything populated,
	 *
	 * @param featureCode
	 * @param enterpriseName
	 * @param identityToken
	 */
	public GeographyFeatureCode create(GeographyFeatureCode featureCode, IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		IClassification<?> classification = classificationService.find(FeatureCodes, enterprise, identityToken);
		classificationService.create(featureCode.getCode(), featureCode.getDescription(), FeatureCodes.concept(), geoSystem, (short) 0, classification, identityToken);
		return featureCode;
	}
	
	
	@Override
	public void loadFeatureCodes(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		progressMonitor.setTotalTasks(681);
		try (GeoDataFinder finder = new GeoDataFinder(FeatureCodes_en, CSVFormat.TDF, FeatureCodes_en.getHeaderNames()))
		{
			for (CSVRecord record : finder.getRecords())
			{
				GeographyFeatureCode featureCode = new GeographyFeatureCode();
				featureCode.setCode(record.get("code"));
				featureCode.setDescription(record.get("description"));
				create(featureCode, enterpriseName, identityToken);
				
				IClassification<?> featureClass = classificationService.find(featureCode.getClassClassification()
				                                                                        .classificationName(), enterprise, identityToken);
				IClassification<?> featureCodeClassification = classificationService.find(featureCode.getCode(), FeatureCodes.concept(), enterprise, identityToken);
				featureClass.addChild(featureCodeClassification, enterprise, identityToken);
				
				logProgress("Geography Feature Codes", "Loaded Feature Code - " + featureCode.toString(), 1, progressMonitor);
			}
		}
	}
	
	@Override
	@CacheResult(cacheName = "GeographyfindFeatureCode")
	public GeographyFeatureCode findFeatureCode(@CacheKey String featureCode, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IClassification<?> fClass = findFeatureCodeClassification(featureCode, enterpriseName, identityToken);
		GeographyFeatureCode fCode = new GeographyFeatureCode().setCode(fClass.getName())
		                                                       .setDescription(fClass.getDescription());
		return fCode;
	}
	
	@Override
	@CacheResult(cacheName = "GeographyfindFeatureCodeClassification")
	public IClassification<?> findFeatureCodeClassification(@CacheKey String featureCode, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = get(IClassificationService.class);
		return classificationService.find(featureCode, enterprise, identityToken);
	}
	
	@Override
	public void loadTownsAndCities(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		
		Map<Long, GeoNameDefaultData<?>> dataMap = new TreeMap<>();
		Map<Long, List<Long>> hierarchyMap = new ConcurrentHashMap<>();
		
		progressMonitor.setTotalTasks(102850);
		try (GeoDataFinder finder = new GeoDataFinder(ZAGeoData, CSVFormat.TDF, ZAGeoData.getHeaderNames()))
		{
			int count = 0;
			for (CSVRecord a : finder.getRecords())
			{
				count++;
				String featureCode = a.get("feature code");
				if (Strings.isNullOrEmpty(featureCode))
				{ featureCode = "Z.UKN"; }
				else
				{
					featureCode = a.get("feature class") + "." + featureCode;
				}
				GeoNameDefaultData<?> data = new GeoNameDefaultData<>();
				data.setFeatureCode(findFeatureCode(featureCode, enterpriseName, identityToken));
				data.setFeatureClass(data.getFeatureCode()
				                         .getClassClassification());
				
				if (!EnumSet.of(GeographyFeatureClassesClassifications.A,
				                GeographyFeatureClassesClassifications.P)
				            .contains(data.getFeatureClass()))
				{
					if (count % 50 == 0)
					{
						logProgress("Geography Data", "Skipped Non-Place Location - " + a.get("asciiname") +
								" (" + progressMonitor.getCurrentTask() + "/" + progressMonitor.getTotalTasks() + ")", 50, progressMonitor);
					}
					continue;
				}
				if (!Strings.isNullOrEmpty(a.get("admin1 code")))
				{ data.setAdmin1Code(new GeographyAsciiCode().setCode(a.get("admin1 code"))); }
				if (!Strings.isNullOrEmpty(a.get("admin2 code")))
				{ data.setAdmin2Code(new GeographyAsciiCode().setCode(a.get("admin2 code"))); }
				if (!Strings.isNullOrEmpty(a.get("admin3 code")))
				{ data.setAdmin3Code(a.get("admin3 code")); }
				if (!Strings.isNullOrEmpty(a.get("admin4 code")))
				{ data.setAdmin4Code(a.get("admin4 code")); }
				
				data.setAsciiname(a.get("asciiname"));
				data.setName(a.get("name"));
				data.setGeonameId(Long.parseLong(a.get("geonameid")));
				data.setCountryCode(findCountry(new GeographyCountry().setIso(a.get("country code")), enterpriseName, identityToken));
				data.setCoordinates(new GeographyCoordinates(a.get("latitude"), a.get("longitude")));
				
				if (!Strings.isNullOrEmpty(a.get("dem")))
				{ data.setDem(Integer.parseInt(a.get("dem"))); }
				if (!Strings.isNullOrEmpty(a.get("elevation")))
				{ data.setElevation(Integer.parseInt(a.get("elevation"))); }
				if (!Strings.isNullOrEmpty(a.get("population")))
				{ data.setPopulation(Integer.parseInt(a.get("population"))); }
				
				dataMap.put(data.getGeonameId(), data);
				
				if (count % 50 == 0)
				{
					logProgress("Geography Data", "Loaded - " + a.get("asciiname") +
							" (" + progressMonitor.getCurrentTask() + "/" + progressMonitor.getTotalTasks() + ")", 50, progressMonitor);
				}
			}
		}
		
		progressMonitor.setCurrentTask(0);
		progressMonitor.setTotalTasks(488058);
		try (GeoDataFinder finder = new GeoDataFinder(Hierarchy, CSVFormat.TDF, Hierarchy.getHeaderNames()))
		{
			int count = 0;
			for (CSVRecord record : finder.getRecords())
			{
				count++;
				if (!"ADM".equals(record.get("type")))
				{ continue; }
				addToMap(Long.valueOf(record.get(0)), Long.valueOf(record.get(1)), hierarchyMap);
				if (count % 50 == 0)
				{
					logProgress("Geography Data", "Loaded Hierarchy Data - " + record.get(0) +
							" (" + progressMonitor.getCurrentTask() + "/" + progressMonitor.getTotalTasks() + ")", 50, progressMonitor);
				}
			}
		}
		
		logProgress("Geography Data", "Cleaning up hierarchy for data map..." +
				" (" + progressMonitor.getCurrentTask() + "/" + progressMonitor.getTotalTasks() + ")", 1, progressMonitor);
		
		hierarchyMap.entrySet()
		            .iterator()
		            .forEachRemaining(a -> {
			            if (!dataMap.containsKey(a.getKey()))
			            { hierarchyMap.remove(a.getKey()); }
		            });
		
		logProgress("Geography Data", "Mapping Districts to Places..." +
				" (" + progressMonitor.getCurrentTask() + "/" + progressMonitor.getTotalTasks() + ")", 1, progressMonitor);
		dataMap.forEach((key, value) -> {
			if (value.getAdmin2Code() != null)
			{
				DistrictService districtService = get(DistrictService.class);
				IGeography<?> district = districtService.findDistrict(
						value.getCountryCode()
						     .getIso() + "." +
								value.getAdmin1Code()
								     .getCode() + "." +
								value.getAdmin2Code()
								     .getCode(), enterprise, identityToken);
				if (!hierarchyMap.containsKey(Long.valueOf(district.getOriginalSourceSystemUniqueID())))
				{ hierarchyMap.put(Long.valueOf(district.getOriginalSourceSystemUniqueID()), new ArrayList<>()); }
				if (!hierarchyMap.get(Long.valueOf(district.getOriginalSourceSystemUniqueID()))
				                 .contains(value.getGeonameId()))
				{
					hierarchyMap.get(Long.valueOf(district.getOriginalSourceSystemUniqueID()))
					            .add(value.getGeonameId());
				}
			}
		});
		
		DistrictService districtService = get(DistrictService.class);
		ProvinceService provinceService = get(ProvinceService.class);
		List<Geography> allDistricts = districtService.findAllDistricts(enterprise, identityToken);
		
		progressMonitor.setCurrentTask(0);
		progressMonitor.setTotalTasks(hierarchyMap.size());
		logProgress("Geo Hierarchy", "Loading Structure... " + hierarchyMap.size() + " in total", 1, progressMonitor);
		loadHierarchyLevel(hierarchyMap, dataMap, allDistricts, enterprise, progressMonitor, identityToken);
		
	}
	
	private void loadHierarchyLevel(Map<Long, List<Long>> hierarchyMap, Map<Long, GeoNameDefaultData<?>> dataMap,
	                                List<Geography> geoList, IEnterprise<?> enterprise,
	                                IActivityMasterProgressMonitor progressMonitor,
	                                UUID... identityToken)
	{
		for (int i = 0; i < geoList.size(); i++)
		{
			IGeography<?> iGeography = geoList.get(i);
			if (!Strings.isNullOrEmpty(iGeography.getOriginalSourceSystemUniqueID()))
			{
				if (hierarchyMap.get(Long.valueOf(iGeography.getOriginalSourceSystemUniqueID())) != null)
				{
					for (Long aLong : hierarchyMap.get(Long.valueOf(iGeography.getOriginalSourceSystemUniqueID())))
					{
						if (aLong == null)
						{ continue; }
						GeoNameDefaultData<?> geoNameDefaultData = create(dataMap.get(aLong), get(ClassificationService.class).find(Town, enterprise, identityToken),
						                                                  enterprise.getIEnterprise(),
						                                                  identityToken);
						IGeography<?> newChild = findGeographyByID(geoNameDefaultData.getGeographyId());
						if (newChild == null)
						{ continue; }
						iGeography.addChild(newChild, STRING_EMPTY, enterprise, identityToken);
						if (hierarchyMap.get(geoNameDefaultData.getGeonameId()) != null)
						{
							for (Long againLong : hierarchyMap.get(geoNameDefaultData.getGeonameId()))
							{
								if (againLong == null)
								{ continue; }
								GeoNameDefaultData<?> againNameDefaultData = create(dataMap.get(againLong), get(ClassificationService.class).find(Town, enterprise, identityToken),
								                                                    enterprise.getIEnterprise(),
								                                                    identityToken);
								IGeography<?> againChild = findGeographyByID(againNameDefaultData.getGeographyId());
								if (againChild == null)
								{ continue; }
								iGeography.addChild(againChild, STRING_EMPTY, enterprise, identityToken);
							}
						}
					}
				}
			}
			
			logProgress("Geo Hierarchy", "Loading Structure... (" + i + "/" + geoList.size() + ")", 1, progressMonitor);
		}
	}
	
	private void addToMap(Long id, Long child, Map<Long, List<Long>> map)
	{
		if (!map.containsKey(id))
		{
			map.put(id, new ArrayList<>());
		}
		map.get(id)
		   .add(child);
	}
	
	/**
	 * Created with everything populated,
	 *
	 * @param geoData
	 * @param enterpriseName
	 * @param identityToken
	 */
	public GeoNameDefaultData<?> create(GeoNameDefaultData<?> geoData, IClassification<?> classification, IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = get(IEnterpriseService.class)
				.getEnterprise(enterpriseName);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		
		if (geoData.getGeonameId() == null)
		{ geoData.setGeonameId(-1L); }
		
		Long exists = new Geography()
				.builder()
				.withGeoNameID(geoData.getGeonameId())
				.getCount();
		if (exists == 0)
		{
			Geography geo = new Geography();
			geo.setName(geoData.getName());
			geo.setDescription(geoData.getAsciiname());
			if (geoData.getGeonameId() != -1L)
			{ geo.setOriginalSourceSystemUniqueID(Long.toString(geoData.getGeonameId())); }
			
			geo.setEnterpriseID((Enterprise) enterprise);
			geo.setClassification((Classification) classification);
			geo.setSystemID((Systems) geoSystem);
			geo.setActiveFlagID(((Systems) geoSystem).getActiveFlagID());
			geo.setOriginalSourceSystemID((Systems) geoSystem);
			
			geo.setClassification((Classification) classification);
			
			geo.persist();
			geoData.setGeographyId(geo.getId());
			if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
			{
				geo.createDefaultSecurity(geoSystem, identityToken);
			}
			if (geoData.getCoordinates() != null)
			{
				geo.add(Latitude, geoData.getCoordinates()
				                         .getLatitude(), geoSystem, identityToken);
				geo.add(Longitude, geoData.getCoordinates()
				                          .getLongitude(), geoSystem, identityToken);
			}
			if (geoData.getFeatureCode() != null)
			{
				try
				{
					IClassification<?> featureCode = findFeatureCodeClassification(geoData.getFeatureCode()
					                                                                      .getCode(), enterpriseName, identityToken);
					geo.add(featureCode, "", geoSystem, identityToken);
				}
				catch (NoSuchElementException e)
				{
					//System.out.println("No feature code");
				}
			}
			if (geoData.getFeatureClass() != null)
			{
				try
				{
					IClassification<?> featureClass = findFeatureClass(geoData.getFeatureClass(), enterpriseName, identityToken);
					geo.add(featureClass, "", geoSystem, identityToken);
				}
				catch (NoSuchElementException e)
				{
					//	System.out.println("No feature code");
				}
			}
			
			if (geoData.getPopulation() != 0)
			{ geo.add(Population, Integer.toString(geoData.getPopulation()), geoSystem, identityToken); }
			if (geoData.getElevation() != 0)
			{ geo.add(Elevation, Integer.toString(geoData.getElevation()), geoSystem, identityToken); }
			if (geoData.getDem() != 0)
			{ geo.add(DEM, Integer.toString(geoData.getDem()), geoSystem, identityToken); }
		}
		return geoData;
	}
	
	@CacheResult(cacheName = "GeographyByGeoNameID")
	public Geography findGeographyByID(@CacheKey Long geographyID)
	{
		if (geographyID == null)
		{ return null; }
		return new Geography().builder()
		                      .find(geographyID)
		                      .get()
		                      .orElseThrow();
	}
}
