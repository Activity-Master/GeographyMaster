package com.guicedee.activitymaster.geography;

/**
 * Reactivity Migration Checklist:
 *
 * [✓] One action per Mutiny.Session at a time
 * [✓] Pass Mutiny.Session through the chain
 * [✓] No await() usage
 * [✓] No parallel operations on a session
 * [✓] No session/transaction creation in libraries
 */

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.guicedee.activitymaster.fsdm.client.services.IActiveFlagService;
import com.guicedee.activitymaster.fsdm.client.services.IClassificationService;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.classifications.IClassification;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.geography.IGeography;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.classifications.EnterpriseClassificationDataConcepts;
import com.guicedee.activitymaster.fsdm.client.services.systems.IProgressable;
import com.guicedee.activitymaster.fsdm.db.entities.classifications.Classification;
import com.guicedee.activitymaster.fsdm.db.entities.geography.Geography;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.*;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.client.IGuiceContext;
import geodata.GeoDataFinder;
import geodata.GeoDataLocation;
import geodata.GeoNamesDownloader;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.reactive.mutiny.Mutiny;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.guicedee.activitymaster.fsdm.client.services.administration.ActivityMasterConfiguration.applicationEnterpriseName;
import static com.guicedee.activitymaster.fsdm.client.services.classifications.DefaultClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.modules.services.jsonrepresentation.json.StaticStrings.*;
import static geodata.GeoDataFiles.*;

@Log4j2
@Singleton
public class GeographyService
		implements IProgressable,
		           IGeographyService<GeographyService>
{
	@Inject
	private PlanetService planetService;

	@Inject
	private ContinentService continentService;

	@Inject
	private CountryService countryService;

	@Inject
	private ProvinceService provinceService;

	@Inject
	private DistrictService districtService;

	@Inject
	private TownService townService;

	@Inject
	private PostalCodeService postalCodeService;

	@Inject
	private CurrencyService currencyService;

	@Inject
	private TimeZoneService timeZoneService;

	@Inject
	private LanguagesService languagesService;

	@Inject
	private IClassificationService<?> classificationService;

	@Override
	public Uni<IGeography<?, ?>> createPlanet(Mutiny.Session session, @NotNull String value, String originalUniqueID, ISystems<?, ?> system, UUID... identityToken)
	{
		return planetService.createPlanet(session, value, "The planet " + value, originalUniqueID, system, identityToken);
	}

	@Override
	public Uni<IGeography<?, ?>> createContinent(Mutiny.Session session, String planetName, GeographyContinent continent, ISystems<?, ?> originatingSystem, String originalUniqueID, UUID... identityToken)
	{
		return planetService.findPlanet(session, planetName, originatingSystem, identityToken)
			.chain(planet -> continentService.createContinent(session, planet, continent.getContinentCode(), continent.getContinentName(), originalUniqueID, originatingSystem, identityToken));
	}

	@Override
	public Uni<IGeography<?, ?>> findPlanet(Mutiny.Session session, String name, ISystems<?, ?> originatingSystem, UUID... identityToken)
	{
		return planetService.findPlanet(session, name, originatingSystem, identityToken);
	}

	@Override
	public Uni<GeographyContinent> findContinent(Mutiny.Session session, GeographyContinent continent, ISystems<?, ?> system, UUID... identityToken)
	{
		return continentService.findContinent(session, continent.getContinentCode(), system, identityToken)
			.map(continentGeo -> {
				GeographyContinent gc = new GeographyContinent();
				gc.setContinentCode(continentGeo.getName());
				gc.setContinentName(continentGeo.getDescription());
				gc.setGeographyId(continentGeo.getId());
				return gc;
			});
	}

	@Override
	public Uni<GeographyCountry> findCountry(Mutiny.Session session, GeographyCountry country, ISystems<?, ?> system, UUID... identityToken)
	{
		return countryService.findCountry(session, country.getIso(), system, identityToken)
			.chain(geo -> {
				GeographyCountry gc = new GeographyCountry();
				gc.setGeographyId(geo.getId());
				gc.setIso(country.getIso());
				return geo.findClassification(session, GeoNameID, system, identityToken)
					.onItem().ifNotNull().invoke(rel -> gc.setGeonameId(Long.parseLong(rel.getValue())))
					.replaceWith(gc);
			});
	}

	public Uni<GeographyCountry> createCountry(Mutiny.Session session, GeographyCountry country, ISystems<?, ?> system, UUID... identityToken)
	{
		return continentService.findContinent(session, country.getContinent().getContinentCode(), system, identityToken)
			.chain(geoContinent -> countryService.createCountry(session, geoContinent, country.getIso(), country.getCountryName(), country.getGeonameId() + "", system, identityToken))
			.chain(geoCountry -> {
				return currencyService.createCurrency(session, country.getCurrency().getCurrencyCode(), country.getCurrency().getCurrencyName(), system, identityToken)
					.chain(currency -> {
						return geoCountry.addOrUpdateClassification(session, Currency, currency.getName(), system, identityToken)
							.chain(() -> countryService.updateCountry(session, currency, country.getIso(), country.getCountryName(), country.getIso3(), country.getIsoNumeric(),
								country.getCountryDialCode(), country.getFips(), country.getCapital(), country.getAreaSqlKM(),
								country.getPostalCodeDecimalFormat(), country.getPostalCodeRegexFormat(),
								country.getPopulation(), country.getWebTld(), system, identityToken));
					})
					.map(updated -> {
						country.setGeographyId(geoCountry.getId());
						return country;
					});
			});
	}

	@Override
	public Uni<GeographyCountry> findCountryDetailed(Mutiny.Session session, String iso, ISystems<?, ?> system, UUID... identityToken)
	{
		return countryService.findCountry(session, iso, system, identityToken)
			.chain(geo -> {
				GeographyCountry gc = new GeographyCountry();
				gc.setGeographyId(geo.getId());
				gc.setIso(geo.getName());
				gc.setCountryName(geo.getDescription());
				// Read-only hydration: fetch every classification for this country in a single
				// security-checked query rather than chaining a round-trip per field. Hibernate
				// Reactive forbids parallel operations on a shared session, so batching (one query)
				// is the safe equivalent of "doing them in parallel".
				return geo.findClassificationValues(session, system, identityToken)
					.invoke(values -> {
						String geoName = values.get(GeoNameID.toString());
						if (geoName != null && !geoName.isBlank())
						{
							try { gc.setGeonameId(Long.parseLong(geoName.trim())); }
							catch (NumberFormatException ignored) { /* leave null */ }
						}
						gc.setIso3(values.get(CountryISO3166_3.toString()));
						gc.setIsoNumeric(values.get(CountryISO_Numeric.toString()));
						gc.setFips(values.get(CountryFips.toString()));
						gc.setCapital(values.get(CountryCapital.toString()));
						gc.setAreaSqlKM(values.get(CountryAreaInSqKm.toString()));
						gc.setWebTld(values.get(CountryTld.toString()));
						gc.setCountryDialCode(values.get(CountryPhone.toString()));
						gc.setPostalCodeDecimalFormat(values.get(CountryPostalCodeFormat.toString()));
						gc.setPostalCodeRegexFormat(values.get(CountryPostalCodeRegex.toString()));
						String population = values.get(Population.toString());
						if (population != null && !population.isBlank())
						{
							try { gc.setPopulation(Integer.parseInt(population.trim())); }
							catch (NumberFormatException ignored) { /* leave 0 */ }
						}
					})
					.replaceWith(gc);
			});
	}

	/**
	 * Reads a single classification value attached to the given geography row, returning {@code null}
	 * when the relationship is absent (rather than failing the chain).
	 */

	@Override
	public Uni<Void> loadProvincesASCII1(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken)
	{
		setCurrentTask(0);
		setTotalTasks(4470);

		return Uni.createFrom().item(() -> {
			try (GeoDataFinder finder = new GeoDataFinder(Admin1CodesASCII, CSVFormat.TDF, Admin1CodesASCII.getHeaderNames()))
			{
				List<GeographyAsciiCode> records = new ArrayList<>();
				for (CSVRecord record : finder.getRecords())
				{
					GeographyAsciiCode ascii = new GeographyAsciiCode();
					ascii.setCode(record.get(0))
						.setName(record.get(1))
						.setNameAscii(record.get(2))
						.setGeonameId(Long.parseLong(record.get(3)));
					if (ascii.getCode().startsWith(countryCode))
					{
						records.add(ascii);
					}
				}
				return records;
			}
			catch (Exception e)
			{
				log.error("Error loading province codes", e);
				throw new RuntimeException("Error loading province codes", e);
			}
		})
		.chain(records -> {
			Uni<Void> chain = Uni.createFrom().voidItem();
			for (GeographyAsciiCode ascii : records)
			{
				chain = chain.chain(() -> countryService.findCountry(session, countryCode, system)
					.chain(country -> provinceService.createProvince(session, country, ascii.getCode(), ascii.getName(), ascii.getGeonameId() + "", system))
					.invoke(province -> logProgress("Geography Service", "Loaded Province Codes - " + ascii.getName(), 1))
					.replaceWithVoid());
			}
			return chain;
		})
		.invoke(() -> logProgress("Geography Service", "Finished Province Codes", 0))
		.onFailure().invoke(error -> log.error("Error loading province codes: {}", error.getMessage(), error));
	}

	@Override
	public Uni<Void> loadDistrictsASCII2(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken)
	{
		setCurrentTask(0);
		setTotalTasks(47850);

		return Uni.createFrom().item(() -> {
			try (GeoDataFinder finder = new GeoDataFinder(Admin2Codes, CSVFormat.TDF, Admin2Codes.getHeaderNames()))
			{
				List<GeographyAsciiCode> records = new ArrayList<>();
				for (CSVRecord record : finder.getRecords())
				{
					GeographyAsciiCode ascii = new GeographyAsciiCode();
					ascii.setCode(record.get(0))
						.setName(record.get(1))
						.setNameAscii(record.get(2))
						.setGeonameId(Long.parseLong(record.get(3)));
					int provinceCodeDecimalLocation = ascii.getCode().indexOf('.', 4);
					String provinceCode = ascii.getCode().substring(0, provinceCodeDecimalLocation);
					if (provinceCode.startsWith(countryCode))
					{
						records.add(ascii);
					}
				}
				return records;
			}
			catch (Exception e)
			{
				log.error("Error loading district codes", e);
				throw new RuntimeException("Error loading district codes", e);
			}
		})
		.chain(records -> {
			Uni<Void> chain = Uni.createFrom().voidItem();
			for (GeographyAsciiCode ascii : records)
			{
				int provinceCodeDecimalLocation = ascii.getCode().indexOf('.', 4);
				String provinceCode = ascii.getCode().substring(0, provinceCodeDecimalLocation);
				chain = chain.chain(() -> provinceService.findProvince(session, provinceCode, system)
					.chain(province -> districtService.createDistrict(session, province, ascii.getCode(), ascii.getName(), ascii.getGeonameId() + "", system))
					.invoke(district -> logProgress("Geography Service", "Loaded District - " + ascii.getName(), 1))
					.replaceWithVoid());
			}
			return chain;
		})
		.invoke(() -> logProgress("Geography Service", "Finished Districts/Cities", 0))
		.onFailure().invoke(error -> log.error("Error loading district codes: {}", error.getMessage(), error));
	}

	@Override
	public Uni<Void> loadLanguages(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{
		setCurrentTask(0);
		setTotalTasks(547);

		return Uni.createFrom().item(() -> {
			try (GeoDataFinder finder = new GeoDataFinder(ISO639Languages, CSVFormat.TDF, ISO639Languages.getHeaderNames()))
			{
				List<ISO639Language> languages = new ArrayList<>();
				for (CSVRecord record : finder.getRecords())
				{
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
						while (st.hasMoreTokens()) language.getName().add(st.nextToken());
					}
					if (!french.isEmpty())
					{
						StringTokenizer st = new StringTokenizer(french, ";");
						while (st.hasMoreTokens()) language.getFrenchName().add(st.nextToken());
					}
					if (!german.isEmpty())
					{
						StringTokenizer st = new StringTokenizer(german, ";");
						while (st.hasMoreTokens()) language.getGermanName().add(st.nextToken());
					}
					languages.add(language);
				}
				return languages;
			}
			catch (Exception e)
			{
				log.error("Error loading languages", e);
				throw new RuntimeException("Error loading languages", e);
			}
		})
		.chain(languages -> {
			Uni<Void> chain = Uni.createFrom().voidItem();
			for (ISO639Language language : languages)
			{
				chain = chain.chain(() -> {
					String english = language.getName().isEmpty() ? "" : language.getName().iterator().next();
					return languagesService.createLanguage(session, language.getIso6391Code(), english, language.getIso6391Code(), system)
						.chain(lang -> languagesService.updateLanguage(session, lang.getName(), null, language.getIso6392Code(), null, null, null, system))
						.chain(updated -> {
							Uni<?> nameChain = Uni.createFrom().voidItem();
							for (String s : language.getName())
								nameChain = nameChain.chain(() -> languagesService.updateLanguage(session, updated.getName(), null, null, s, null, null, system).replaceWithVoid());
							for (String s : language.getFrenchName())
								nameChain = nameChain.chain(() -> languagesService.updateLanguage(session, updated.getName(), null, null, null, s, null, system).replaceWithVoid());
							for (String s : language.getGermanName())
								nameChain = nameChain.chain(() -> languagesService.updateLanguage(session, updated.getName(), null, null, null, null, s, system).replaceWithVoid());
							return nameChain.replaceWithVoid();
						})
						.invoke(() -> logProgress("Geography Service", "Loading Language - " + language.getIso6391Code(), 1));
				});
			}
			return chain;
		})
		.invoke(() -> logProgress("Geography Service", "Geography Associated Languages queued", 1))
		.onFailure().invoke(error -> log.error("Error loading languages: {}", error.getMessage(), error));
	}

	@Override
	public Uni<Void> loadCountryInfo(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{

		setCurrentTask(0);
		setTotalTasks(252);

		return Uni.createFrom().item(() -> {
			try (GeoDataFinder finder = new GeoDataFinder(CountryInfo, CSVFormat.TDF, CountryInfo.getHeaderNames()))
			{
				List<CSVRecord> records = new ArrayList<>();
				for (CSVRecord record : finder.getRecords()) records.add(record);
				return records;
			}
			catch (Exception e)
			{
				log.error("Error loading country info", e);
				throw new RuntimeException("Error loading country info", e);
			}
		})
		.chain(records -> {
			Uni<Void> chain = Uni.createFrom().voidItem();
			for (CSVRecord record : records)
			{
				chain = chain.chain(() -> {
					GeographyCountry country = new GeographyCountry();
					country.setIso(record.get(0));
					country.setIso3(record.get(1));
					country.setIsoNumeric(record.get(2));
					country.setFips(record.get(3));
					country.setCountryName(record.get(4));
					country.setCapital(record.get(5));
					country.setAreaSqlKM(record.get(6));
					try { country.setPopulation(Integer.parseInt(record.get(7))); }
					catch (NumberFormatException nfe) { country.setPopulation(0); }

					String continentCode = record.get(8);
					return findContinent(session, new GeographyContinent().setContinentCode(continentCode), system, identityToken)
						.chain(gc -> {
							country.setContinent(gc);
							country.setWebTld(record.get(9));
							return currencyService.createCurrency(session, record.get(10), record.get(11), system, identityToken);
						})
						.chain(currencyClassification -> {
							GeographyCurrency gcc = new GeographyCurrency().setCurrencyCode(currencyClassification.getName())
								.setCurrencyName(currencyClassification.getDescription());
							country.setCurrency(gcc);
							country.setCountryDialCode(record.get(12));
							country.setPostalCodeDecimalFormat(record.get(13));
							country.setPostalCodeRegexFormat(record.get(14));
							try { country.setGeonameId(Long.parseLong(record.get(16))); }
							catch (NumberFormatException nfe) { /* skip */ }
							if (record.size() > 18) country.setEquivalentFips(record.get(18));
							return createCountry(session, country, system, identityToken);
						})
						.invoke(gccc -> logProgress("Geography Service", "Loaded Country " + country.getCountryName(), 1))
						.replaceWithVoid();
				});
			}
			return chain;
		})
		.invoke(() -> logProgress("Geography Service", "Finished Loading Countries", 10))
		.onFailure().invoke(error -> log.error("Error loading country info: {}", error.getMessage(), error));
	}

	@Override
	public Uni<GeographyTimezone> findTimezone(Mutiny.Session session, GeographyTimezone timezone, ISystems<?, ?> system, UUID... identityToken)
	{
		return timeZoneService.findTimeZone(session, timezone.getTimezoneID(), system, identityToken)
			.map(timeZoneClassification -> {
				GeographyTimezone tz = new GeographyTimezone();
				tz.setTimezoneID(timeZoneClassification.getName());
				return tz;
			});
	}

	@Override
	public Uni<Void> loadTimeZones(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{
		setCurrentTask(0);
		setTotalTasks(425);

		return Uni.createFrom().item(() -> {
			try (GeoDataFinder finder = new GeoDataFinder(TimeZones, CSVFormat.TDF, TimeZones.getHeaderNames()))
			{
				List<CSVRecord> records = new ArrayList<>();
				for (CSVRecord record : finder.getRecords()) records.add(record);
				return records;
			}
			catch (Exception e)
			{
				log.error("Error loading time zones", e);
				throw new RuntimeException("Error loading time zones", e);
			}
		})
		.chain(records -> {
			Uni<Void> chain = Uni.createFrom().voidItem();

			for (CSVRecord record : records)
			{
				chain = chain.chain(() -> {
					GeographyTimezone timezone = new GeographyTimezone();
					timezone.setTimezoneID(record.get(1));
					timezone.setOffsetJan2016(Double.parseDouble(record.get(2)));
					timezone.setOffsetJuly2016(Double.parseDouble(record.get(3)));
					timezone.setRawOffset(Double.parseDouble(record.get(4)));

					return createTimezone(session, timezone, system, identityToken)
						.chain(() -> countryService.findCountry(session, record.get(0), system))
						.chain(country -> {
							return timeZoneService.findTimeZone(session, timezone.getTimezoneID(), system, identityToken)
								.chain(timeZone -> country.addOrUpdateClassification(session, TimeZone, timeZone.getName(), system, identityToken));
						})
						.invoke(() -> logProgress("TimeZones", "Loaded Timezone - " + timezone.getTimezoneID(), 1))
						.replaceWithVoid();
				});
			}
			return chain;
		})
		.onFailure().invoke(error -> log.error("Error loading time zones: {}", error.getMessage(), error));
	}

	public Uni<GeographyTimezone> createTimezone(Mutiny.Session session, GeographyTimezone timezone, ISystems<?, ?> system, UUID... identityToken)
	{
		return timeZoneService.createTimeZone(session, timezone.getTimezoneID(), timezone.getTimezoneID(), null, system, identityToken)
			.chain(() -> timeZoneService.updateTimeZone(session, timezone.getTimezoneID(), null,
				timezone.getRawOffset() + "", timezone.getOffsetJuly2016() + "", timezone.getOffsetJan2016() + "",
				system, identityToken))
			.map(updated -> timezone);
	}

	@Override
	public Uni<Void> loadPostalCodes(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{

		return Uni.createFrom().item(() -> {
			Map<Long, List<GeographyPostalCode>> postalCodeMap = new HashMap<>();
			setCurrentTask(0);
			setTotalTasks(3921);
			try (GeoDataFinder finder = new GeoDataFinder(ZAPostalCodes, CSVFormat.TDF, ZAPostalCodes.getHeaderNames()))
			{
				for (CSVRecord record : finder.getRecords())
				{
					GeographyPostalCode post = new GeographyPostalCode();
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
							postalCodeMap.get(Long.valueOf(post.getPostalCode())).add(post);
						}
					}
				}
			}
			catch (Exception e)
			{
				log.error("Error loading postal codes", e);
				throw new RuntimeException("Error loading postal codes", e);
			}
			return postalCodeMap;
		})
		.chain(postalCodeMap -> {
			Uni<Void> chain = Uni.createFrom().voidItem();
			setTotalTasks(postalCodeMap.size());
			setCurrentTask(0);

			for (Map.Entry<Long, List<GeographyPostalCode>> entry : postalCodeMap.entrySet())
			{
				Long key = entry.getKey();
				List<GeographyPostalCode> value = entry.getValue();

				chain = chain.chain(() -> {
					if (value.isEmpty())
					{
						log.warn("Unknown Postal Code for district? - {}", key);
						return Uni.createFrom().voidItem();
					}
					GeographyPostalCode gp = value.get(0);
					if (value.size() > 1) gp.setProvinceName(value.get(1).getProvinceName());

					return townService.findTown(session, gp.getParentPlaceName(), system, identityToken)
						.onFailure().recoverWithUni(error -> {
							if (!Strings.isNullOrEmpty(gp.getProvinceName()))
							{
								return districtService.findFirstDistrictInProvince(session, gp.getProvinceName(), system, identityToken)
									.chain(firstDistrict -> {
										if (firstDistrict == null) return Uni.createFrom().nullItem();
										return townService.createTown(session, firstDistrict,
											gp.getParentPlaceName(), gp.getParentPlaceName(),
											gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()),
											system, identityToken);
									});
							}
							log.warn("Cannot resolve town for postal code - {}", gp);
							return Uni.createFrom().nullItem();
						})
						.chain(town -> {
							if (town == null) return Uni.createFrom().voidItem();
							return postalCodeService.createPostalCode(session, town, gp.getPostalCode(), gp.getPostalCodePlaceName(),
								gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()), system, identityToken)
								.chain(postalCode -> {
									Uni<Void> subChain = Uni.createFrom().voidItem();
									for (GeographyPostalCode geographyPostalCode : value)
									{
										subChain = subChain.chain(() -> postalCodeService.createPostalCodeSuburb(session, postalCode,
											geographyPostalCode.getPostalCode(), geographyPostalCode.getPostalCodePlaceName(),
											geographyPostalCode.getPostalCode(), system, identityToken).replaceWithVoid());
									}
									return subChain;
								})
								.invoke(() -> logProgress("Postal Codes", "Loaded PostalCode - " + gp.getPostalCode(), 1));
						});
				});
			}
			return chain;
		})
		.onFailure().invoke(error -> log.error("Error loading postal codes: {}", error.getMessage(), error));
	}

	@Override
	public Uni<GeographyPostalCode> findPostalCode(Mutiny.Session session, GeographyPostalCode postalCode, ISystems<?, ?> system, UUID... identityToken)
	{
		return postalCodeService.findPostalCode(session, null, postalCode.getPostalCode(), system, identityToken)
			.map(geo -> {
				GeographyPostalCode result = new GeographyPostalCode();
				result.setGeographyId(geo.getId());
				result.setPostalCodePlaceName(geo.getDescription());
				return result;
			});
	}

	@Override
	public Uni<GeographyPostalCode> findPostalCodeSuburb(Mutiny.Session session, String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		return postalCodeService.findPostalCodeSuburb(session, code, description, system, identityToken)
			.map(geo -> {
				GeographyPostalCode result = new GeographyPostalCode();
				result.setGeographyId(geo.getId());
				result.setPostalCodePlaceName(geo.getDescription());
				return result;
			});
	}

	@Override
	public Uni<GeographyPostalCode> findOrCreatePostalCodeSuburb(Mutiny.Session session, String code, String description, ISystems<?, ?> system, UUID... identityToken)
	{
		return postalCodeService.findOrCreatePostalCodeSuburb(session, code, description, system, identityToken)
			.map(geo -> {
				GeographyPostalCode result = new GeographyPostalCode();
				result.setGeographyId(geo.getId());
				result.setPostalCodePlaceName(geo.getDescription());
				return result;
			});
	}

	@Override
	public Uni<IGeography<?, ?>> findGeographyById(Mutiny.Session session, UUID geographyID, ISystems<?, ?> system, UUID... identityToken)
	{
		return session.find(Geography.class, geographyID)
			.onItem().ifNull().failWith(() -> new GeographyException("Geography not found: " + geographyID))
			.map(geo -> (IGeography<?, ?>) geo);
	}

	public Uni<IClassification<?, ?>> findFeatureClass(Mutiny.Session session, GeographyFeatureClassesClassifications key, ISystems<?, ?> system, UUID... identityToken)
	{
		return classificationService.find(session, key.toString(), system, identityToken);
	}

	public Uni<GeographyFeatureCode> create(Mutiny.Session session, GeographyFeatureCode featureCode, ISystems<?, ?> system, UUID... identityToken)
	{
		return classificationService.find(session, FeatureCodes.toString(), system, identityToken)
			.chain(classification -> classificationService.create(session, featureCode.getCode(), featureCode.getDescription(), FeatureCodes.concept(), system, 0, classification, identityToken))
			.map(created -> featureCode);
	}

	@Override
	public Uni<Void> loadFeatureCodes(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{
		setCurrentTask(0);
		setTotalTasks(681);

		return Uni.createFrom().item(() -> {
			try (GeoDataFinder finder = new GeoDataFinder(FeatureCodes_en, CSVFormat.TDF, FeatureCodes_en.getHeaderNames()))
			{
				List<CSVRecord> records = new ArrayList<>();
				for (CSVRecord record : finder.getRecords()) records.add(record);
				return records;
			}
			catch (Exception e)
			{
				log.error("Error loading feature codes", e);
				throw new RuntimeException("Error loading feature codes", e);
			}
		})
		.chain(records -> {
			Uni<Void> chain = Uni.createFrom().voidItem();
			for (CSVRecord record : records)
			{
				chain = chain.chain(() -> {
					GeographyFeatureCode featureCode = new GeographyFeatureCode();
					featureCode.setCode(record.get("code"));
					featureCode.setDescription(record.get("description"));
					return create(session, featureCode, system, identityToken)
						.chain(fc -> classificationService.find(session, fc.getClassClassification().toString(), system, identityToken))
						.chain(clazz -> classificationService.find(session, featureCode.getCode(), FeatureCodes.concept(), system, identityToken)
							.chain(featureCodeClassification -> {
								@SuppressWarnings("unchecked")
								IClassification<Classification, ?> pp = (IClassification<Classification, ?>) clazz;
								return pp.addChild(session, (Classification) featureCodeClassification, NoClassification.toString(), null, system, identityToken);
							}))
						.invoke(() -> logProgress("Geography Feature Codes", "Loaded Feature Code - " + featureCode.toString(), 1))
						.replaceWithVoid();
				});
			}
			return chain;
		})
		.onFailure().invoke(error -> log.error("Error loading feature codes: {}", error.getMessage(), error));
	}

	@Override
	public Uni<GeographyFeatureCode> findFeatureCode(Mutiny.Session session, String featureCode, ISystems<?, ?> system, UUID... identityToken)
	{
		return findFeatureCodeClassification(session, featureCode, system, identityToken)
			.map(fClass -> new GeographyFeatureCode().setCode(fClass.getName()).setDescription(fClass.getDescription()));
	}

	@Override
	public Uni<IClassification<?, ?>> findFeatureCodeClassification(Mutiny.Session session, String featureCode, ISystems<?, ?> system, UUID... identityToken)
	{
		return classificationService.find(session, featureCode, system, identityToken);
	}

	@Override
	public Uni<Void> loadTownsAndCities(Mutiny.Session session, ISystems<?, ?> system, UUID... identityToken)
	{

		return Uni.createFrom().item(() -> {
			Map<Long, GeoNameDefaultData<?>> dataMap = new TreeMap<>();
			Map<Long, List<Long>> hierarchyMap = new ConcurrentHashMap<>();

			setTotalTasks(102850);
			try (GeoDataFinder finder = new GeoDataFinder(ZAGeoData, CSVFormat.TDF, ZAGeoData.getHeaderNames()))
			{
				for (CSVRecord a : finder.getRecords())
				{
					String featureCodeStr = a.get("feature code");
					if (Strings.isNullOrEmpty(featureCodeStr)) featureCodeStr = "Z.UKN";
					else featureCodeStr = a.get("feature class") + "." + featureCodeStr;

					GeoNameDefaultData<?> data = new GeoNameDefaultData<>();
					data.setAsciiname(a.get("asciiname"));
					data.setName(a.get("name"));
					data.setGeonameId(Long.parseLong(a.get("geonameid")));
					data.setCoordinates(new GeographyCoordinates(a.get("latitude"), a.get("longitude")));
					if (!Strings.isNullOrEmpty(a.get("dem"))) data.setDem(Integer.parseInt(a.get("dem")));
					if (!Strings.isNullOrEmpty(a.get("elevation"))) data.setElevation(Integer.parseInt(a.get("elevation")));
					if (!Strings.isNullOrEmpty(a.get("population"))) data.setPopulation(Integer.parseInt(a.get("population")));
					if (!Strings.isNullOrEmpty(a.get("admin1 code"))) data.setAdmin1Code(new GeographyAsciiCode().setCode(a.get("admin1 code")));
					if (!Strings.isNullOrEmpty(a.get("admin2 code"))) data.setAdmin2Code(new GeographyAsciiCode().setCode(a.get("admin2 code")));
					if (!Strings.isNullOrEmpty(a.get("admin3 code"))) data.setAdmin3Code(a.get("admin3 code"));
					if (!Strings.isNullOrEmpty(a.get("admin4 code"))) data.setAdmin4Code(a.get("admin4 code"));

					// Store feature code string for later resolution
					data.setAdmin3Code(featureCodeStr); // temporary reuse field for feature code string

					dataMap.put(data.getGeonameId(), data);
				}
			}
			catch (Exception e)
			{
				log.error("Error loading towns and cities data", e);
				throw new RuntimeException("Error loading towns and cities data", e);
			}

			try (GeoDataFinder finder = new GeoDataFinder(Hierarchy, CSVFormat.TDF, Hierarchy.getHeaderNames()))
			{
				for (CSVRecord record : finder.getRecords())
				{
					if (!"ADM".equals(record.get("type"))) continue;
					Long parentId = Long.valueOf(record.get(0));
					Long childId = Long.valueOf(record.get(1));
					hierarchyMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
				}
			}
			catch (Exception e)
			{
				log.error("Error loading hierarchy data", e);
				throw new RuntimeException("Error loading hierarchy data", e);
			}

			return new Object[]{dataMap, hierarchyMap};
		})
		.chain(data -> {
			@SuppressWarnings("unchecked")
			Map<Long, GeoNameDefaultData<?>> dataMap = (Map<Long, GeoNameDefaultData<?>>) data[0];
			@SuppressWarnings("unchecked")
			Map<Long, List<Long>> hierarchyMap = (Map<Long, List<Long>>) data[1];

			// Remove hierarchy entries not in dataMap
			hierarchyMap.entrySet().removeIf(entry -> !dataMap.containsKey(entry.getKey()));

			log.info("Loading {} hierarchy entries for towns and cities", hierarchyMap.size());

			return districtService.findAllDistricts(session, system, identityToken)
				.chain(allDistricts -> {
					Uni<Void> chain = Uni.createFrom().voidItem();
					for (Geography iGeography : allDistricts)
					{
						chain = chain.chain(() -> iGeography.findClassification(session, GeoNameID, system, identityToken)
							.chain(rel -> {
								String geoIdStr = rel != null ? rel.getValue() : null;
								if (Strings.isNullOrEmpty(geoIdStr)) return Uni.createFrom().voidItem();
								Long geoId = Long.valueOf(geoIdStr);
								List<Long> children = hierarchyMap.get(geoId);
								if (children == null) return Uni.createFrom().voidItem();

								Uni<Void> childChain = Uni.createFrom().voidItem();
								for (Long childId : children)
								{
									GeoNameDefaultData<?> childData = dataMap.get(childId);
									if (childData == null) continue;
									childChain = childChain.chain(() -> classificationService.find(session, Town.toString(), system, identityToken)
										.chain(townClassification -> createGeoData(session, childData, townClassification, system, identityToken))
										.chain(created -> {
											if (created.getGeographyId() == null) return Uni.createFrom().voidItem();
											return session.find(Geography.class, created.getGeographyId())
												.chain(newChild -> {
													if (newChild == null) return Uni.createFrom().voidItem();
													return iGeography.addChild(session, newChild, NoClassification.toString(), STRING_EMPTY, system, identityToken)
														.replaceWithVoid();
												});
										}));
								}
								return childChain;
							}));
					}
					return chain;
				});
		})
		.onFailure().invoke(error -> log.error("Error loading towns and cities: {}", error.getMessage(), error));
	}

	private Uni<GeoNameDefaultData<?>> createGeoData(Mutiny.Session session, GeoNameDefaultData<?> geoData, IClassification<?, ?> classification,
	                                                 ISystems<?, ?> system, UUID... identityToken)
	{
		if (geoData.getGeonameId() == null) geoData.setGeonameId(-1L);

		return SessionUtils.withActivityMaster(applicationEnterpriseName, system.getName(), tuple -> {
			var createSession = tuple.getItem1();
			var createEnterprise = tuple.getItem2();
			var createSystem = tuple.getItem3();
			var createIdentityToken = tuple.getItem4();

			Geography geo = new Geography();
			return geo.builder(createSession)
				.withGeoNameID(geoData.getGeonameId().toString())
				.getCount()
				.chain(count -> {
					if (count > 0)
					{
						return Uni.createFrom().item(geoData);
					}

					geo.setName(geoData.getName());
					geo.setDescription(geoData.getAsciiname());
					geo.setEnterpriseID(createEnterprise);
					geo.setClassificationID((Classification) classification);
					geo.setSystemID(createSystem);
					geo.setOriginalSourceSystemID(createSystem.getId());

					IActiveFlagService<?> acService = IGuiceContext.get(IActiveFlagService.class);
					return acService.getActiveFlag(createSession, createEnterprise, createIdentityToken)
						.chain(activeFlag -> {
							geo.setActiveFlagID(activeFlag);
							return createSession.persist(geo).replaceWith(Uni.createFrom().item(geo));
						})
						.chain(persisted -> {
							geoData.setGeographyId(geo.getId());
							Uni<?> setupChain = geo.createDefaultSecurity(createSession, createSystem, createIdentityToken)
								.onFailure().recoverWithItem(() -> null);
							// Store geoname ID as a classification
							if (geoData.getGeonameId() != null && geoData.getGeonameId() != -1L)
							{
								setupChain = setupChain.chain(() -> geo.addClassification(createSession, GeoNameID.toString(), Long.toString(geoData.getGeonameId()), createSystem, createIdentityToken));
							}
							return setupChain;
						})
						.chain(secResult -> {
							Uni<?> classChain = Uni.createFrom().voidItem();
							if (geoData.getCoordinates() != null)
							{
								classChain = classChain.chain(() -> geo.addClassification(createSession, Latitude.toString(), geoData.getCoordinates().getLatitude(), createSystem, createIdentityToken))
									.chain(() -> geo.addClassification(createSession, Longitude.toString(), geoData.getCoordinates().getLongitude(), createSystem, createIdentityToken));
							}
							if (geoData.getPopulation() != 0)
								classChain = classChain.chain(() -> geo.addClassification(createSession, Population.toString(), Integer.toString(geoData.getPopulation()), createSystem, createIdentityToken));
							if (geoData.getElevation() != 0)
								classChain = classChain.chain(() -> geo.addClassification(createSession, Elevation.toString(), Integer.toString(geoData.getElevation()), createSystem, createIdentityToken));
							if (geoData.getDem() != 0)
								classChain = classChain.chain(() -> geo.addClassification(createSession, DEM.toString(), Integer.toString(geoData.getDem()), createSystem, createIdentityToken));
							return classChain.replaceWith(geoData);
						});
				});
		});
	}

	public Uni<Geography> findGeographyByID(Mutiny.Session session, UUID geographyID)
	{
		if (geographyID == null) return Uni.createFrom().nullItem();
		return session.find(Geography.class, geographyID);
	}

	// ---------------------------------------------------------------------------------------------
	//  GeoNames download + per-country installation
	// ---------------------------------------------------------------------------------------------

	@Override
	public Uni<Void> downloadCountryData(String countryCode, UUID... identityToken)
	{
		String cc = countryCode.toUpperCase();
		return Uni.createFrom().<Void>item(() -> {
				try
				{
					GeoNamesDownloader downloader = new GeoNamesDownloader();
					logProgress("Geography Download", "Downloading GeoNames reference data");
					downloader.downloadReferenceData();
					logProgress("Geography Download", "Downloading GeoNames data for " + cc);
					downloader.downloadCountry(cc);
					log.info("GeoNames data for {} downloaded into {}", cc, GeoDataLocation.getBaseDirectory().toAbsolutePath());
					return null;
				}
				catch (IOException e)
				{
					log.error("Failed to download GeoNames data for {}: {}", cc, e.getMessage(), e);
					throw new RuntimeException("Failed to download GeoNames data for " + cc, e);
				}
			})
			.runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
	}

	@Override
	public Uni<Void> installCountry(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken)
	{
		String cc = countryCode.toUpperCase();
		log.info("Installing country {} into ActivityMaster", cc);
		return ensureCountryFilesDownloaded(cc)
			.chain(() -> loadProvincesASCII1(session, system, cc, identityToken))
			.chain(() -> loadDistrictsASCII2(session, system, cc, identityToken))
			.chain(() -> loadCountryGeoData(session, system, cc, identityToken))
			.chain(() -> loadCountryPostalCodes(session, system, cc, identityToken))
			.invoke(() -> log.info("Finished installing country {}", cc))
			.onFailure().invoke(error -> log.error("Error installing country {}: {}", cc, error.getMessage(), error));
	}

	/**
	 * Ensures the per-country GeoNames files exist on disk, downloading them on a worker thread when absent.
	 */
	private Uni<Void> ensureCountryFilesDownloaded(String cc)
	{
		Path geoData = GeoDataLocation.countryGeoDataFile(cc);
		Path postalCodes = GeoDataLocation.countryPostalCodeFile(cc);
		if (Files.exists(geoData) && Files.exists(postalCodes))
		{
			log.info("GeoNames files for {} already present in {}", cc, GeoDataLocation.getBaseDirectory().toAbsolutePath());
			return Uni.createFrom().voidItem();
		}
		return downloadCountryData(cc);
	}

	@Override
	public Uni<Void> loadCountryGeoData(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken)
	{
		String cc = countryCode.toUpperCase();
		Path geoDataFile = GeoDataLocation.countryGeoDataFile(cc);

		return Uni.createFrom().item(() -> {
			Map<Long, GeoNameDefaultData<?>> dataMap = new TreeMap<>();
			Map<Long, List<Long>> hierarchyMap = new ConcurrentHashMap<>();

			if (!Files.exists(geoDataFile))
			{
				throw new GeographyException("GeoNames geo-data file not found for " + cc + " at " + geoDataFile.toAbsolutePath()
					+ " - download it first via downloadCountryData(\"" + cc + "\")");
			}

			try (GeoDataFinder finder = new GeoDataFinder(geoDataFile, CSVFormat.TDF, ZAGeoData.getHeaderNames()))
			{
				for (CSVRecord a : finder.getRecords())
				{
					String featureCodeStr = a.get("feature code");
					if (Strings.isNullOrEmpty(featureCodeStr)) featureCodeStr = "Z.UKN";
					else featureCodeStr = a.get("feature class") + "." + featureCodeStr;

					GeoNameDefaultData<?> data = new GeoNameDefaultData<>();
					data.setAsciiname(a.get("asciiname"));
					data.setName(a.get("name"));
					data.setGeonameId(Long.parseLong(a.get("geonameid")));
					data.setCoordinates(new GeographyCoordinates(a.get("latitude"), a.get("longitude")));
					if (!Strings.isNullOrEmpty(a.get("dem"))) data.setDem(Integer.parseInt(a.get("dem")));
					if (!Strings.isNullOrEmpty(a.get("elevation"))) data.setElevation(Integer.parseInt(a.get("elevation")));
					if (!Strings.isNullOrEmpty(a.get("population"))) data.setPopulation(Integer.parseInt(a.get("population")));
					if (!Strings.isNullOrEmpty(a.get("admin1 code"))) data.setAdmin1Code(new GeographyAsciiCode().setCode(a.get("admin1 code")));
					if (!Strings.isNullOrEmpty(a.get("admin2 code"))) data.setAdmin2Code(new GeographyAsciiCode().setCode(a.get("admin2 code")));
					if (!Strings.isNullOrEmpty(a.get("admin3 code"))) data.setAdmin3Code(a.get("admin3 code"));
					if (!Strings.isNullOrEmpty(a.get("admin4 code"))) data.setAdmin4Code(a.get("admin4 code"));

					// Store feature code string for later resolution
					data.setAdmin3Code(featureCodeStr);

					dataMap.put(data.getGeonameId(), data);
				}
			}
			catch (GeographyException ge)
			{
				throw ge;
			}
			catch (Exception e)
			{
				log.error("Error loading geo-data for {}", cc, e);
				throw new RuntimeException("Error loading geo-data for " + cc, e);
			}

			loadHierarchyInto(hierarchyMap);

			setTotalTasks(dataMap.size());
			return new Object[]{dataMap, hierarchyMap};
		})
		.chain(data -> {
			@SuppressWarnings("unchecked")
			Map<Long, GeoNameDefaultData<?>> dataMap = (Map<Long, GeoNameDefaultData<?>>) data[0];
			@SuppressWarnings("unchecked")
			Map<Long, List<Long>> hierarchyMap = (Map<Long, List<Long>>) data[1];

			hierarchyMap.entrySet().removeIf(entry -> !dataMap.containsKey(entry.getKey()));
			log.info("Loading {} geo-data hierarchy entries for {}", hierarchyMap.size(), cc);

			return districtService.findAllDistricts(session, system, identityToken)
				.chain(allDistricts -> {
					Uni<Void> chain = Uni.createFrom().voidItem();
					for (Geography iGeography : allDistricts)
					{
						chain = chain.chain(() -> iGeography.findClassification(session, GeoNameID, system, identityToken)
							.chain(rel -> {
								String geoIdStr = rel != null ? rel.getValue() : null;
								if (Strings.isNullOrEmpty(geoIdStr)) return Uni.createFrom().voidItem();
								Long geoId = Long.valueOf(geoIdStr);
								List<Long> children = hierarchyMap.get(geoId);
								if (children == null) return Uni.createFrom().voidItem();

								Uni<Void> childChain = Uni.createFrom().voidItem();
								for (Long childId : children)
								{
									GeoNameDefaultData<?> childData = dataMap.get(childId);
									if (childData == null) continue;
									childChain = childChain.chain(() -> classificationService.find(session, Town.toString(), system, identityToken)
										.chain(townClassification -> createGeoData(session, childData, townClassification, system, identityToken))
										.chain(created -> {
											if (created.getGeographyId() == null) return Uni.createFrom().voidItem();
											return session.find(Geography.class, created.getGeographyId())
												.chain(newChild -> {
													if (newChild == null) return Uni.createFrom().voidItem();
													return iGeography.addChild(session, newChild, NoClassification.toString(), STRING_EMPTY, system, identityToken)
														.replaceWithVoid();
												});
										})
										.invoke(() -> logProgress("Geography Geo-Data", "Loaded place - " + childData.getName(), 1)));
								}
								return childChain;
							}));
					}
					return chain;
				});
		})
		.invoke(() -> logProgress("Geography Geo-Data", "Finished loading geo-data for " + cc, 0))
		.onFailure().invoke(error -> log.error("Error loading geo-data for {}: {}", cc, error.getMessage(), error));
	}

	/**
	 * Loads the hierarchy entries (ADM only) into the supplied map, preferring the downloaded
	 * {@code hierarchy.txt} and falling back to the bundled {@code hierarchy.csv} resource.
	 */
	private void loadHierarchyInto(Map<Long, List<Long>> hierarchyMap)
	{
		Path downloadedHierarchy = GeoDataLocation.dumpDirectory().resolve("hierarchy.txt");
		if (Files.exists(downloadedHierarchy))
		{
			try (GeoDataFinder finder = new GeoDataFinder(downloadedHierarchy, CSVFormat.TDF, Hierarchy.getHeaderNames()))
			{
				collectHierarchy(finder, hierarchyMap);
				return;
			}
			catch (Exception e)
			{
				log.warn("Failed to read downloaded hierarchy file, falling back to bundled resource: {}", e.getMessage());
			}
		}
		try (GeoDataFinder finder = new GeoDataFinder(Hierarchy, CSVFormat.TDF, Hierarchy.getHeaderNames()))
		{
			collectHierarchy(finder, hierarchyMap);
		}
		catch (Exception e)
		{
			log.error("Error loading hierarchy data", e);
			throw new RuntimeException("Error loading hierarchy data", e);
		}
	}

	private void collectHierarchy(GeoDataFinder finder, Map<Long, List<Long>> hierarchyMap)
	{
		for (CSVRecord record : finder.getRecords())
		{
			if (!"ADM".equals(record.get("type"))) continue;
			Long parentId = Long.valueOf(record.get(0));
			Long childId = Long.valueOf(record.get(1));
			hierarchyMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
		}
	}

	@Override
	public Uni<Void> loadCountryPostalCodes(Mutiny.Session session, ISystems<?, ?> system, String countryCode, UUID... identityToken)
	{
		String cc = countryCode.toUpperCase();
		Path postalCodeFile = GeoDataLocation.countryPostalCodeFile(cc);

		return Uni.createFrom().item(() -> {
			Map<Long, List<GeographyPostalCode>> postalCodeMap = new HashMap<>();
			setCurrentTask(0);

			if (!Files.exists(postalCodeFile))
			{
				throw new GeographyException("GeoNames postal-code file not found for " + cc + " at " + postalCodeFile.toAbsolutePath()
					+ " - download it first via downloadCountryData(\"" + cc + "\")");
			}

			try (GeoDataFinder finder = new GeoDataFinder(postalCodeFile, CSVFormat.TDF, ZAPostalCodes.getHeaderNames()))
			{
				for (CSVRecord record : finder.getRecords())
				{
					String postalCodeValue = record.get(1);
					if (Strings.isNullOrEmpty(postalCodeValue)) continue;
					Long key;
					try { key = Long.valueOf(postalCodeValue); }
					catch (NumberFormatException nfe) { continue; }

					GeographyPostalCode post = new GeographyPostalCode();
					post.setPostalCode(postalCodeValue);
					post.setPostalCodePlaceName(record.get(2));
					post.setParentPlaceName(record.get(2));
					post.setCoordinates(new GeographyCoordinates(record.get(9), record.get(10)));
					if (record.size() > 3 && !Strings.isNullOrEmpty(record.get(3)))
					{
						post.setProvinceName(record.get(3));
					}

					postalCodeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(post);
				}
			}
			catch (GeographyException ge)
			{
				throw ge;
			}
			catch (Exception e)
			{
				log.error("Error loading postal codes for {}", cc, e);
				throw new RuntimeException("Error loading postal codes for " + cc, e);
			}
			return postalCodeMap;
		})
		.chain(postalCodeMap -> {
			Uni<Void> chain = Uni.createFrom().voidItem();
			setTotalTasks(postalCodeMap.size());
			setCurrentTask(0);

			for (Map.Entry<Long, List<GeographyPostalCode>> entry : postalCodeMap.entrySet())
			{
				Long key = entry.getKey();
				List<GeographyPostalCode> value = entry.getValue();

				chain = chain.chain(() -> {
					if (value.isEmpty())
					{
						log.warn("Unknown Postal Code for district? - {}", key);
						return Uni.createFrom().voidItem();
					}
					GeographyPostalCode gp = value.get(0);
					if (value.size() > 1) gp.setProvinceName(value.get(1).getProvinceName());

					return townService.findTown(session, gp.getParentPlaceName(), system, identityToken)
						.onFailure().recoverWithUni(error -> {
							if (!Strings.isNullOrEmpty(gp.getProvinceName()))
							{
								return districtService.findFirstDistrictInProvince(session, gp.getProvinceName(), system, identityToken)
									.chain(firstDistrict -> {
										if (firstDistrict == null) return Uni.createFrom().nullItem();
										return townService.createTown(session, firstDistrict,
											gp.getParentPlaceName(), gp.getParentPlaceName(),
											gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()),
											system, identityToken);
									});
							}
							log.warn("Cannot resolve town for postal code - {}", gp);
							return Uni.createFrom().nullItem();
						})
						.chain(town -> {
							if (town == null) return Uni.createFrom().voidItem();
							return postalCodeService.createPostalCode(session, town, gp.getPostalCode(), gp.getPostalCodePlaceName(),
								gp.getGeonameId() == null ? "" : Long.toString(gp.getGeonameId()), system, identityToken)
								.chain(postalCode -> {
									Uni<Void> subChain = Uni.createFrom().voidItem();
									for (GeographyPostalCode geographyPostalCode : value)
									{
										subChain = subChain.chain(() -> postalCodeService.createPostalCodeSuburb(session, postalCode,
											geographyPostalCode.getPostalCode(), geographyPostalCode.getPostalCodePlaceName(),
											geographyPostalCode.getPostalCode(), system, identityToken).replaceWithVoid());
									}
									return subChain;
								})
								.invoke(() -> logProgress("Postal Codes", "Loaded PostalCode - " + gp.getPostalCode(), 1));
						});
				});
			}
			return chain;
		})
		.invoke(() -> logProgress("Postal Codes", "Finished loading postal codes for " + cc, 0))
		.onFailure().invoke(error -> log.error("Error loading postal codes for {}: {}", cc, error.getMessage(), error));
	}
}
