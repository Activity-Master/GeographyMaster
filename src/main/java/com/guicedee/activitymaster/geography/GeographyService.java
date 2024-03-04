package com.guicedee.activitymaster.geography;

import com.google.common.base.Strings;
import com.guicedee.activitymaster.fsdm.ClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationDataConceptService;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.annotations.ActivityMasterDB;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts;
import com.guicedee.activitymaster.fsdm.client.services.systems.IProgressable;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.fsdm.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.geography.implementations.GeographySystem;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.*;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedpersistence.db.annotations.Transactional;
import com.guicedee.logger.LogFactory;
import geodata.GeoDataFinder;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.client.IGuiceContext.*;
import static com.guicedee.guicedinjection.json.StaticStrings.*;
import static geodata.GeoDataFiles.*;

public class GeographyService
		implements IProgressable,
		           IGeographyService<GeographyService>
{
	@Override
	
	public IGeography<?,?> createPlanet(@CacheKey @NotNull String value, String originalUniqueID, ISystems<?,?> system, java.util.UUID... identityToken)
	{
		PlanetService service = get(PlanetService.class);
		return service.createPlanet(value, "The planet " + value, originalUniqueID, system, identityToken);
	}
	
	@Override
	public IGeography<?,?> createContinent(String planetName, GeographyContinent continent, ISystems<?,?> originatingSystem, String originalUniqueID, java.util.UUID... identityToken)
	{
		PlanetService planetService = get(PlanetService.class);
		IGeography<Geography,GeographyQueryBuilder> planet = planetService.findPlanet(planetName, originatingSystem, identityToken);
		ContinentService service = get(ContinentService.class);
		return service.createContinent(planet, continent.getContinentCode(), continent.getContinentName(), originalUniqueID, originatingSystem, identityToken);
	}
	
	
	@Override
	@CacheResult
	public IGeography<?,?> findPlanet(@CacheKey String name, @CacheKey ISystems<?,?> originatingSystem, @CacheKey java.util.UUID... identityToken)
	{
		PlanetService service = get(PlanetService.class);
		return service.findPlanet(name, originatingSystem, identityToken);
	}
	
	
	@Override
	@CacheResult
	public GeographyContinent findContinent(@CacheKey GeographyContinent continent, @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		ContinentService service = get(ContinentService.class);
		IGeography<?,?> continentGeo = service.findContinent(continent.getContinentCode(), system, identityToken);
		GeographyContinent gc = new GeographyContinent();
		gc.setContinentCode(continentGeo.getName());
		gc.setContinentName(continentGeo.getDescription());
		gc.setGeographyId(continentGeo.getId());
		return gc;
	}
	
	@Override
	@CacheResult
	public GeographyCountry findCountry(@CacheKey GeographyCountry country, @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		CountryService cs = get(CountryService.class);
		IGeography<?,?> geo = cs.findCountry(country.getIso(), system, identityToken);
		GeographyCountry gc = new GeographyCountry();
		
		List<Object[]> values = geo.builder().getClassificationsValuePivot(CountryISO3166.toString(), (String)null, system, identityToken,
				CountryISO3166_3.toString(),
				CountryISO_Numeric.toString(),
				CountryFips.toString(),
				CountryCapital.toString(),
				CountryAreaInSqKm.toString(),
				CountryTld.toString(),
				CountryPhone.toString(),
				CountryPostalCodeFormat.toString(),
				CountryPostalCodeRegex.toString());
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
		
		if (geo.hasClassifications(Currency,null, system, identityToken))
		{
			String currency = geo.findClassification(Currency, system, identityToken)
			                     .orElseThrow()
			                     .getValue();
			CurrencyService currencyService = get(CurrencyService.class);
			IClassification<?,?> geoCurrency = currencyService.findCurrency(currency, system, identityToken);
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
	
	public GeographyCountry createCountry(GeographyCountry country, ISystems<?,?> system, java.util.UUID... identityToken)
	{
		CountryService countryService = get(CountryService.class);
		IGeography<Geography,GeographyQueryBuilder> geoContinent = get(ContinentService.class).findContinent(country.getContinent()
		                                                                                                                                                           .getContinentCode(), system, identityToken);
		var geoCountry =
				countryService.createCountry(geoContinent, country.getIso(), country.getCountryName(), country.getGeonameId() + "", system, identityToken);
		
		
		IClassification<?,?> currency = get(ClassificationService.class).find(country.getCurrency()
		                                                                             .getCurrencyCode(), EnterpriseClassificationDataConcepts.ClassificationXClassification, system, identityToken);
		geoCountry.addOrUpdateClassification(Currency, currency.getName(), system, identityToken);
		
		
		countryService.updateCountry(currency, country.getIso(), country.getCountryName(), country.getIso3(), country.getIsoNumeric(), country.getCountryDialCode(),
				country.getFips(), country.getCapital(), country.getAreaSqlKM(), country.getPostalCodeDecimalFormat(), country.getPostalCodeRegexFormat(),
				country.getPopulation(), country.getWebTld(), system, identityToken);
		
		country.setGeographyId(geoCountry.getId());
		return country;
	}
	
	@Override
	public void loadProvincesASCII1(ISystems<?,?> system, String countryCode)
	{
		setCurrentTask(0);
		setTotalTasks(4470);
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
				if(ascii.getCode().startsWith(countryCode))
				{
					ProvinceService provinceService = get(ProvinceService.class);
					CountryService countryService = get(CountryService.class);
					IGeography<Geography,GeographyQueryBuilder> country = countryService.findCountry(countryCode, system);
					IGeography<?,?> province = provinceService.createProvince(country, ascii.getCode(), ascii.getName(), ascii.getGeonameId() + "", system);
				}
				if (current % 50 == 0)
				{
					logProgress("Geography Service", "Loaded Province Codes - " + ascii.getName(), 50);
				}
			}
			logProgress("Geography Service", "Finished Province Codes", 0);
		}
	}
	
	@Override
	public void loadDistrictsASCII2(ISystems<?,?> system, String countryCode)
	{
		setCurrentTask(0);
		setTotalTasks(47850);
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
				
					int proviceCodeDecimalLocation = ascii.getCode()
				                                      .indexOf('.', 4);
				
					String provinceCode = ascii.getCode()
					                           .substring(0, proviceCodeDecimalLocation);
					
					if(provinceCode.startsWith(countryCode))
					{
						ProvinceService provinceService = get(ProvinceService.class);
						IGeography<Geography, GeographyQueryBuilder> province = provinceService.findProvince(provinceCode, system);
						DistrictService districtService = get(DistrictService.class);
						districtService.createDistrict(province, ascii.getCode(), ascii.getName(), ascii.getGeonameId() + "", system);
					}
				if (current % 50 == 0)
				{
					logProgress("Geography Service", "Loaded 50 Districts/Cities - " + ascii.getName(), 50);
				}
			}
		}
		logProgress("Geography Service", "Finished Districts/Cities", 10);
	}
	
	@Override
	public void loadLanguages(ISystems<?,?> system)
	{
		setCurrentTask(0);
		setTotalTasks(547);
		try (GeoDataFinder finder = new GeoDataFinder(ISO639Languages, CSVFormat.TDF, ISO639Languages.getHeaderNames()))
		{
			int current = 0;
			logProgress("Geography Service", "Starting Geography Associated Languages", 1);
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
				IClassification<?,?> lang = languagesService.createLanguage(language.getIso6391Code(), english, language.getIso6391Code(), system);
				languagesService.updateLanguage(lang.getName(), null, language.getIso6392Code(), null, null, null, system);
				
				for (String s : language.getName())
				{
					languagesService.updateLanguage(lang.getName(), null, null, s, null, null, system);
				}
				for (String s : language.getFrenchName())
				{
					languagesService.updateLanguage(lang.getName(), null, null, null, s, null, system);
				}
				for (String s : language.getGermanName())
				{
					languagesService.updateLanguage(lang.getName(), null, null, null, null, s, system);
				}
				
				if (current % 5 == 0)
				{
					logProgress("Geography Service", "Loading Language - " +
									(language.getName()
									         .isEmpty() ? " - " : language.getName()
									                                      .toArray()[0])
							, 5);
				}
			}
		}
		logProgress("Geography Service", "Geography Associated Languages queued", 1);
	}
	
	
	@Override
	public void loadCountryInfo(ISystems<?,?> system)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);
		UUID identityToken = com.guicedee.client.IGuiceContext.get(GeographySystem.class).getSystemToken(system.getEnterprise());
		
		setCurrentTask(0);
		setTotalTasks(252);
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
				GeographyContinent gc = findContinent(new GeographyContinent().setContinentCode(continentCode), system, identityToken);
				country.setContinent(gc);
				
				country.setWebTld(record.get(9));
				
				IClassification<?,?> currencyClassification = get(CurrencyService.class).createCurrency(record.get(10), record.get(11), system, identityToken);
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
				
				GeographyCountry gccc = createCountry(country, system);
				logProgress("Geography Service", "Loaded Country " + country.getCountryName(), 1);
			}
		}
		logProgress("Geography Service", "Finished Loading Countries", 10);
	}
	
	/**
	 * By timezone ID
	 * <p>
	 * like Asia/Baghdad
	 *
	 * @param timezone
	 * @param system
	 *
	 * @return
	 */
	@Override
	@CacheResult(cacheName = "GeographyTimezones")
	public GeographyTimezone findTimezone(@CacheKey GeographyTimezone timezone, @CacheKey ISystems<?,?> system)
	{
		UUID identityToken = com.guicedee.client.IGuiceContext.get(GeographySystem.class).getSystemToken(system.getEnterprise());
		TimeZoneService timeZoneService = get(TimeZoneService.class);
		
		IClassification<?,?> timeZoneClassification = timeZoneService.findTimeZone(timezone.getTimezoneID(), system, identityToken);
		GeographyTimezone tz = new GeographyTimezone();
		tz.setTimezoneID(timeZoneClassification.getName());
		List<Object[]> values = timeZoneClassification.builder().getClassificationsValuePivot(TimeZoneRawOffset.toString(),(String) null, system, new UUID[]{identityToken},
				TimeZoneOffsetJuly2016.toString(),
				TimeZoneOffsetJan2016.toString());
		if (!values.isEmpty())
		{
			if (values.get(0)[0] != null)
			{
				tz.setRawOffset(Double.parseDouble(values.get(0)[0].toString()));
			}
			if (values.get(0)[1] != null)
			{
				tz.setOffsetJuly2016(Double.parseDouble(values.get(0)[1].toString()));
			}
			if (values.get(0)[2] != null)
			{
				tz.setOffsetJan2016(Double.parseDouble(values.get(0)[2].toString()));
			}
		}
		
		return tz;
	}
	
	@Override
	public void loadTimeZones(ISystems<?,?> system)
	{
		setCurrentTask(0);
		setTotalTasks(425);
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
				create(timezone, system);
				
				ISystems<?,?> geoSystem = com.guicedee.client.IGuiceContext.get(GeographySystem.class).getSystem(system.getEnterprise());
				UUID identityToken = com.guicedee.client.IGuiceContext.get(GeographySystem.class).getSystemToken(system.getEnterprise());
				CountryService cs = get(CountryService.class);
				IGeography<?,?> country = cs.findCountry(record.get(0), system);
				
				TimeZoneService timeZoneService = get(TimeZoneService.class);
				IClassification<?,?> timeZone = timeZoneService.findTimeZone(timezone.getTimezoneID(), system, identityToken);
				country.addOrUpdateClassification(TimeZone, timeZone.getName(), geoSystem, identityToken);
				
				if (current % 5 == 0)
				{
					logProgress("TimeZones", "Loaded Timezone - " + timezone.getTimezoneID(), 5);
				}
			}
		}
	}
	
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public GeographyTimezone create(GeographyTimezone timezone, ISystems<?,?> system)
	{
		UUID identityToken = com.guicedee.client.IGuiceContext.get(GeographySystem.class).getSystemToken(system.getEnterprise());
		TimeZoneService timeZoneService = get(TimeZoneService.class);
		timeZoneService.createTimeZone(timezone.getTimezoneID(), timezone.getTimezoneID(), null, system, identityToken);
		
		timeZoneService.updateTimeZone(timezone.getTimezoneID(), null,
				timezone.getRawOffset() + "", timezone.getOffsetJuly2016() + "", timezone.getOffsetJan2016() + "",
				system, identityToken);
		return timezone;
	}
	
	@Override
	public void loadPostalCodes(ISystems<?,?> system)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = get(IClassificationDataConceptService.class);

		UUID identityToken = com.guicedee.client.IGuiceContext.get(GeographySystem.class).getSystemToken(system.getEnterprise());
		//Postal Codes Full Listing
		Map<Long, List<GeographyPostalCode>> postalCodeMap = new HashMap<>();
		setCurrentTask(0);
		setTotalTasks(3921);
		try (GeoDataFinder finder = new GeoDataFinder(ZAPostalCodes, CSVFormat.TDF, ZAPostalCodes.getHeaderNames()))
		{
			int current = 0;
			for (CSVRecord record : finder.getRecords())
			{
				current++;
				GeographyPostalCode post = new GeographyPostalCode();
				String countryCode = record.get(0);
				GeographyCountry cunt = findCountry(new GeographyCountry().setIso(countryCode), system, identityToken);
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
				{
					logProgress("Postal Codes", "Loaded PostalCode - " + post.getPostalCode(), 3);
				}
			}
		}
		
		/*setCurrentTask(0);
		setTotalTasks(19888);
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
				
				GeographyCountry country = findCountry(new GeographyCountry().setIso(countryCode), system, identityToken);
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
				{
					province = "Orange Free State";
				}
				else if (province.equalsIgnoreCase("KwaZulu Natal"))
				{
					province = "KwaZulu-Natal";
				}
				else if (province.equalsIgnoreCase("North West Province"))
				{
					province = "North-West";
				}
				
				
				post.setProvinceName(province);
				
				if (!postalCodeMap.containsKey(Long.valueOf(post.getPostalCode())))
				{
					postalCodeMap.put(Long.valueOf(post.getPostalCode()), new ArrayList<>());
				}
				postalCodeMap.get(Long.valueOf(post.getPostalCode()))
				             .add(post);
				
				if (current % 3 == 0)
				{
					logProgress("Postal Codes", "Loaded PostalCode Updates - " + post.getPostalCode(), 3);
				}
			}
		}*/
		
		DistrictService districtService = get(DistrictService.class);
		PostalCodeService postalCodeService = get(PostalCodeService.class);
		int current = 0;
		setTotalTasks(postalCodeMap.size());
		setCurrentTask(0);
		for (Map.Entry<Long, List<GeographyPostalCode>> entry : postalCodeMap.entrySet())
		{
			Long key = entry.getKey();
			List<GeographyPostalCode> value = entry.getValue();
			current++;
			//Find the town, or the city/district
			TownService townService = get(TownService.class);
			if(value.isEmpty())
			{
				LogFactory.getLog("Geography Service").log(Level.WARNING,"Unknown Postal Code for district? - " + key);
				continue;
			}
			GeographyPostalCode gp = value.get(0);
			if (value.size() > 1)
			{
				gp.setProvinceName(value.get(1)
				                        .getProvinceName());
			}
			IGeography<?,?> town = null;
			try
			{
				town = townService.findTown(gp.getParentPlaceName(), system, identityToken);
			}
			catch (Throwable T)
			{
				if (!Strings.isNullOrEmpty(gp.getProvinceName()))
				{
					try
					{
						IGeography<?,?> firstDistrictInProvince = districtService.findFirstDistrictInProvince(gp.getProvinceName(), system, identityToken);
						town = townService.createTown((IGeography<Geography, GeographyQueryBuilder>) firstDistrictInProvince,
								gp.getParentPlaceName(), gp.getParentPlaceName(),
								gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()),
								system, identityToken);
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
									system,
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
				IGeography<?,?> postalCode = postalCodeService.createPostalCode((IGeography<Geography, GeographyQueryBuilder>) town,
						gp.getPostalCode(), gp.getPostalCodePlaceName(),
						gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()),
						system, identityToken);
				for (GeographyPostalCode geographyPostalCode : value)
				{
					IGeography<?,?> postalCodeSuburb = postalCodeService.createPostalCodeSuburb((IGeography<Geography, GeographyQueryBuilder>)
									postalCode, geographyPostalCode.getPostalCode(),
							geographyPostalCode.getPostalCodePlaceName(),
							geographyPostalCode.getPostalCode(), system, identityToken
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
								system,
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
							system,
							identityToken);
				}
			}
			if (current % 10 == 0)
			{
				logProgress("Postal Codes", "Loaded District->PostalCode Updates - " + gp.getPostalCode(), 10);
			}
		}
		//postalCodeService.createPostalCode();
	}
	
	@Override
	@CacheResult(cacheName = "GeographyPostalCodes")
	public GeographyPostalCode findPostalCode(@CacheKey GeographyPostalCode postalCode, @CacheKey ISystems<?,?> system, java.util.UUID... identityToken)
	{
		PostalCodeService postalCodeService = get(PostalCodeService.class);
		IGeography<?,?> geo = postalCodeService.findPostalCode(null, postalCode.getPostalCode(), system, identityToken);
		GeographyPostalCode result = new GeographyPostalCode();
		result.setGeographyId(geo.getId());
		result.setPostalCodePlaceName(geo.getDescription());
		if (geo.hasClassifications(Latitude,null, system, identityToken))
		{
			String latitude = geo.findClassification(Latitude, system, identityToken)
			                     .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                     .getValue();
			String longitude = geo.findClassification(Longitude, system, identityToken)
			                      .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                      .getValue();
			result.setCoordinates(new GeographyCoordinates(latitude, longitude));
		}
		return result;
	}
	
	
	@Override
	@CacheResult(cacheName = "GeographyPostalCodesSuburb")
	public GeographyPostalCode findPostalCodeSuburb(@CacheKey String code, @CacheKey String description, @CacheKey ISystems<?,?> system, java.util.UUID... identityToken)
	{
		PostalCodeService postalCodeService = get(PostalCodeService.class);
		IGeography<?,?> geo = postalCodeService.findPostalCodeSuburb(code, description, system, identityToken);

		GeographyPostalCode result = new GeographyPostalCode();
		result.setGeographyId(geo.getId());
		result.setPostalCodePlaceName(geo.getDescription());
		if (geo.hasClassifications(Latitude, null,system, identityToken))
		{
			String latitude = geo.findClassification(Latitude, system, identityToken)
			                     .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                     .getValue();
			String longitude = geo.findClassification(Longitude, system, identityToken)
			                      .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                      .getValue();
			result.setCoordinates(new GeographyCoordinates(latitude, longitude));
		}
		return result;
	}
	
	@Override
	@CacheResult(cacheName = "GeographyPostalCodesSuburb")
	public GeographyPostalCode findOrCreatePostalCodeSuburb(@CacheKey String code, @CacheKey String description, @CacheKey ISystems<?,?> system, java.util.UUID... identityToken)
	{
		PostalCodeService postalCodeService = get(PostalCodeService.class);
		IGeography<?,?> geo = postalCodeService.findOrCreatePostalCodeSuburb(code, description, system, identityToken);
		GeographyPostalCode result = new GeographyPostalCode();
		result.setGeographyId(geo.getId());
		result.setPostalCodePlaceName(geo.getDescription());
		if (geo.hasClassifications(Latitude,null, system, identityToken))
		{
			String latitude = geo.findClassification(Latitude, system, identityToken)
			                     .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                     .getValue();
			String longitude = geo.findClassification(Longitude, system, identityToken)
			                      .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
			                      .getValue();
			result.setCoordinates(new GeographyCoordinates(latitude, longitude));
		}
		return result;
	}
	
	@Override
	@CacheResult(cacheName = "GeographyFindGeographyById")
	public IGeography<?,?> findGeographyById(@CacheKey UUID geographyID, @CacheKey ISystems<?,?> system, @CacheKey java.util.UUID... identityToken)
	{
		return new Geography().builder()
		                      .find(geographyID)
		                      .inActiveRange()
		                      .inDateRange()
		                      .withEnterprise(system.getEnterprise())
		                      .get(true)
		                      .orElse(null);
	}
	
	
	@CacheResult(cacheName = "GeographyFindFeatureClass")
	public IClassification<?,?> findFeatureClass(@CacheKey GeographyFeatureClassesClassifications key, @CacheKey ISystems<?,?> system, java.util.UUID... identityToken)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		return classificationService.find(key.toString(), system, identityToken);
	}
	
	/**
	 * Created with everything populated,
	 *
	 * @param featureCode
	 * @param system
	 * @param identityToken
	 */
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public GeographyFeatureCode create(GeographyFeatureCode featureCode, ISystems<?,?> system, java.util.UUID... identityToken)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		IClassification<?,?> classification = classificationService.find(FeatureCodes, system, identityToken);
		classificationService.create(featureCode.getCode(), featureCode.getDescription(), FeatureCodes.concept(), system,  0, classification, identityToken);
		return featureCode;
	}
	
	
	@Override
	public void loadFeatureCodes(ISystems<?,?> system, java.util.UUID... identityToken)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		setCurrentTask(0);
		setTotalTasks(681);
		try (GeoDataFinder finder = new GeoDataFinder(FeatureCodes_en, CSVFormat.TDF, FeatureCodes_en.getHeaderNames()))
		{
			for (CSVRecord record : finder.getRecords())
			{
				GeographyFeatureCode featureCode = new GeographyFeatureCode();
				featureCode.setCode(record.get("code"));
				featureCode.setDescription(record.get("description"));
				create(featureCode, system, identityToken);
				
				Classification clazz
						= (Classification) classificationService.find(featureCode.getClassClassification()
						                                                         .toString(), system, identityToken);
				Classification featureCodeClassification = (Classification) classificationService.find(featureCode.getCode(), FeatureCodes.concept(), system, identityToken);
				clazz.addChild(featureCodeClassification,NoClassification.toString(),null, system, identityToken);
				
				logProgress("Geography Feature Codes", "Loaded Feature Code - " + featureCode.toString(), 1);
			}
		}
	}
	
	@Override
	@CacheResult(cacheName = "GeographyfindFeatureCode")
	public GeographyFeatureCode findFeatureCode(@CacheKey String featureCode, @CacheKey ISystems<?,?> system, java.util.UUID... identityToken)
	{
		IClassification<?,?> fClass = findFeatureCodeClassification(featureCode, system, identityToken);
		GeographyFeatureCode fCode = new GeographyFeatureCode().setCode(fClass.getName())
		                                                       .setDescription(fClass.getDescription());
		return fCode;
	}
	
	@Override
	@CacheResult(cacheName = "GeographyfindFeatureCodeClassification")
	public IClassification<?,?> findFeatureCodeClassification(@CacheKey String featureCode, @CacheKey ISystems<?,?> system, java.util.UUID... identityToken)
	{
		IClassificationService<?> classificationService = get(IClassificationService.class);
		return classificationService.find(featureCode, system, identityToken);
	}
	
	@Override
	public void loadTownsAndCities(ISystems<?,?> system)
	{
		UUID identityToken = com.guicedee.client.IGuiceContext.get(GeographySystem.class).getSystemToken(system.getEnterprise());
		
		Map<Long, GeoNameDefaultData<?>> dataMap = new TreeMap<>();
		Map<Long, List<Long>> hierarchyMap = new ConcurrentHashMap<>();
		
		setTotalTasks(102850);
		try (GeoDataFinder finder = new GeoDataFinder(ZAGeoData, CSVFormat.TDF, ZAGeoData.getHeaderNames()))
		{
			int count = 0;
			for (CSVRecord a : finder.getRecords())
			{
				count++;
				String featureCode = a.get("feature code");
				if (Strings.isNullOrEmpty(featureCode))
				{
					featureCode = "Z.UKN";
				}
				else
				{
					featureCode = a.get("feature class") + "." + featureCode;
				}
				GeoNameDefaultData<?> data = new GeoNameDefaultData<>();
				data.setFeatureCode(findFeatureCode(featureCode, system, identityToken));
				data.setFeatureClass(data.getFeatureCode()
				                         .getClassClassification());
				
				if (!EnumSet.of(GeographyFeatureClassesClassifications.A,
						GeographyFeatureClassesClassifications.P)
				            .contains(data.getFeatureClass()))
				{
					if (count % 50 == 0)
					{
						logProgress("Geography Data", "Skipped Non-Place Location - " + a.get("asciiname") +
								" (" + getCurrentTask() + "/" + getTotalTasks() + ")", 50);
					}
					continue;
				}
				if (!Strings.isNullOrEmpty(a.get("admin1 code")))
				{
					data.setAdmin1Code(new GeographyAsciiCode().setCode(a.get("admin1 code")));
				}
				if (!Strings.isNullOrEmpty(a.get("admin2 code")))
				{
					data.setAdmin2Code(new GeographyAsciiCode().setCode(a.get("admin2 code")));
				}
				if (!Strings.isNullOrEmpty(a.get("admin3 code")))
				{
					data.setAdmin3Code(a.get("admin3 code"));
				}
				if (!Strings.isNullOrEmpty(a.get("admin4 code")))
				{
					data.setAdmin4Code(a.get("admin4 code"));
				}
				
				data.setAsciiname(a.get("asciiname"));
				data.setName(a.get("name"));
				data.setGeonameId(Long.parseLong(a.get("geonameid")));
				data.setCountryCode(findCountry(new GeographyCountry().setIso(a.get("country code")), system, identityToken));
				data.setCoordinates(new GeographyCoordinates(a.get("latitude"), a.get("longitude")));
				
				if (!Strings.isNullOrEmpty(a.get("dem")))
				{
					data.setDem(Integer.parseInt(a.get("dem")));
				}
				if (!Strings.isNullOrEmpty(a.get("elevation")))
				{
					data.setElevation(Integer.parseInt(a.get("elevation")));
				}
				if (!Strings.isNullOrEmpty(a.get("population")))
				{
					data.setPopulation(Integer.parseInt(a.get("population")));
				}
				
				dataMap.put(data.getGeonameId(), data);
				
				if (count % 50 == 0)
				{
					logProgress("Geography Data", "Loaded - " + a.get("asciiname") +
							" (" + getCurrentTask() + "/" + getTotalTasks() + ")", 50);
				}
			}
		}
		
		setCurrentTask(0);
		setTotalTasks(488058);
		try (GeoDataFinder finder = new GeoDataFinder(Hierarchy, CSVFormat.TDF, Hierarchy.getHeaderNames()))
		{
			int count = 0;
			for (CSVRecord record : finder.getRecords())
			{
				count++;
				if (!"ADM".equals(record.get("type")))
				{
					continue;
				}
				addToMap(Long.valueOf(record.get(0)), Long.valueOf(record.get(1)), hierarchyMap);
				if (count % 50 == 0)
				{
					logProgress("Geography Data", "Loaded Hierarchy Data - " + record.get(0) +
							" (" + getCurrentTask() + "/" + getTotalTasks() + ")", 50);
				}
			}
		}
		
		logProgress("Geography Data", "Cleaning up hierarchy for data map..." +
				" (" + getCurrentTask() + "/" + getTotalTasks() + ")", 1);
		
		hierarchyMap.entrySet()
		            .iterator()
		            .forEachRemaining(a -> {
			            if (!dataMap.containsKey(a.getKey()))
			            {
				            hierarchyMap.remove(a.getKey());
			            }
		            });
		
		logProgress("Geography Data", "Mapping Districts to Places..." +
				" (" + getCurrentTask() + "/" + getTotalTasks() + ")", 1);
		dataMap.forEach((key, value) -> {
			if (value.getAdmin2Code() != null)
			{
				DistrictService districtService = get(DistrictService.class);
				String dName = value.getCountryCode()
				                    .getIso() + "." +
				               value.getAdmin1Code()
				                    .getCode() + "." +
				               value.getAdmin2Code()
				                    .getCode();
				try
				{
					IGeography<?, ?> district = districtService.findDistrict(dName, system, identityToken);
					if (!hierarchyMap.containsKey(Long.valueOf(district.getOriginalSourceSystemUniqueID())))
					{
						hierarchyMap.put(Long.valueOf(district.getOriginalSourceSystemUniqueID()), new ArrayList<>());
					}
					if (!hierarchyMap.get(Long.valueOf(district.getOriginalSourceSystemUniqueID()))
					                 .contains(value.getGeonameId()))
					{
						hierarchyMap.get(Long.valueOf(district.getOriginalSourceSystemUniqueID()))
						            .add(value.getGeonameId());
					}
				}catch (Throwable T)
				{
					LogFactory.getLog("Geography Service").log(Level.WARNING,"Cannot find district with code - " + dName);
				}
			}
		});
		
		DistrictService districtService = get(DistrictService.class);
		ProvinceService provinceService = get(ProvinceService.class);
		List<Geography> allDistricts = districtService.findAllDistricts(system, identityToken);
		
		setCurrentTask(0);
		setTotalTasks(hierarchyMap.size());
		logProgress("Geo Hierarchy", "Loading Structure... " + hierarchyMap.size() + " in total", 1);
		loadHierarchyLevel(hierarchyMap, dataMap, allDistricts, system, identityToken);
		
	}
	
	private void loadHierarchyLevel(Map<Long, List<Long>> hierarchyMap, Map<Long, GeoNameDefaultData<?>> dataMap,
	                                List<Geography> geoList, ISystems<?,?> system,
	                                java.util.UUID... identityToken)
	{
		for (int i = 0; i < geoList.size(); i++)
		{
			IGeography<Geography, GeographyQueryBuilder> iGeography = geoList.get(i);
			if (!Strings.isNullOrEmpty(iGeography.getOriginalSourceSystemUniqueID()))
			{
				if (hierarchyMap.get(Long.valueOf(iGeography.getOriginalSourceSystemUniqueID())) != null)
				{
					for (Long aLong : hierarchyMap.get(Long.valueOf(iGeography.getOriginalSourceSystemUniqueID())))
					{
						if (aLong == null)
						{
							continue;
						}
						GeoNameDefaultData<?> geoNameDefaultData = create(dataMap.get(aLong), get(ClassificationService.class).find(Town, system, identityToken),
								system,
								identityToken);
						Geography newChild = findGeographyByID(geoNameDefaultData.getGeographyId());
						if (newChild == null)
						{
							continue;
						}
						iGeography.addChild(newChild,NoClassification.toString(), STRING_EMPTY, system, identityToken);
						if (hierarchyMap.get(geoNameDefaultData.getGeonameId()) != null)
						{
							for (Long againLong : hierarchyMap.get(geoNameDefaultData.getGeonameId()))
							{
								if (againLong == null)
								{
									continue;
								}
								GeoNameDefaultData<?> againNameDefaultData = create(dataMap.get(againLong), get(ClassificationService.class).find(Town, system, identityToken),
										system,
										identityToken);
								Geography againChild = findGeographyByID(againNameDefaultData.getGeographyId());
								if (againChild == null)
								{
									continue;
								}
								iGeography.addChild(againChild,NoClassification.toString(), STRING_EMPTY, system, identityToken);
							}
						}
					}
				}
			}
			
			logProgress("Geo Hierarchy", "Loading Structure... (" + i + "/" + geoList.size() + ")", 1);
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
	 * @param system
	 * @param identityToken
	 */
	//@Transactional(entityManagerAnnotation = ActivityMasterDB.class)
	public GeoNameDefaultData<?> create(GeoNameDefaultData<?> geoData, IClassification<?,?> classification, ISystems<?,?> system, java.util.UUID... identityToken)
	{
		if (geoData.getGeonameId() == null)
		{
			geoData.setGeonameId(-1L);
		}
		
		Long exists = new Geography()
				.builder()
				.withGeoNameID(geoData.getGeonameId()
				                      .toString())
				.getCount();
		if (exists == 0)
		{
			Geography geo = new Geography();
			geo.setName(geoData.getName());
			geo.setDescription(geoData.getAsciiname());
			if (geoData.getGeonameId() != -1L)
			{
				geo.setOriginalSourceSystemUniqueID(Long.toString(geoData.getGeonameId()));
			}
			
			geo.setEnterpriseID(system.getEnterprise());
			geo.setClassification((Classification) classification);
			geo.setSystemID(system);
			geo.setActiveFlagID(system.getActiveFlagID());
			geo.setOriginalSourceSystemID(system);
			
			geo.setClassification((Classification) classification);
			
			geo.persist();
			geoData.setGeographyId(geo.getId());
			
				geo.createDefaultSecurity(system, identityToken);
			
			if (geoData.getCoordinates() != null)
			{
				geo.addClassification(Latitude.toString(), geoData.getCoordinates()
				                         .getLatitude(), system, identityToken);
				geo.addClassification(Longitude.toString(), geoData.getCoordinates()
				                          .getLongitude(), system, identityToken);
			}
			if (geoData.getFeatureCode() != null)
			{
				try
				{
					IClassification<?,?> featureCode = findFeatureCodeClassification(geoData.getFeatureCode()
					                                                                      .getCode(), system, identityToken);
					geo.addClassification(featureCode.getName(), "", system, identityToken);
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
					IClassification<?,?> featureClass = findFeatureClass(geoData.getFeatureClass(), system, identityToken);
					geo.addClassification(featureClass.getName(), "", system, identityToken);
				}
				catch (NoSuchElementException e)
				{
					//	System.out.println("No feature code");
				}
			}
			
			if (geoData.getPopulation() != 0)
			{
				geo.addClassification(Population.toString(), Integer.toString(geoData.getPopulation()), system, identityToken);
			}
			if (geoData.getElevation() != 0)
			{
				geo.addClassification(Elevation.toString(), Integer.toString(geoData.getElevation()), system, identityToken);
			}
			if (geoData.getDem() != 0)
			{
				geo.addClassification(DEM.toString(), Integer.toString(geoData.getDem()), system, identityToken);
			}
		}
		return geoData;
	}
	
	@CacheResult(cacheName = "GeographyByGeoNameID")
	public Geography findGeographyByID(@CacheKey UUID geographyID)
	{
		if (geographyID == null)
		{
			return null;
		}
		return new Geography().builder()
		                      .find(geographyID)
		                      .get()
		                      .orElseThrow();
	}
}
