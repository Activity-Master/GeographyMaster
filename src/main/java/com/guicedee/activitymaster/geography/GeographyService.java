package com.guicedee.activitymaster.geography;

import com.google.inject.Singleton;
import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.ActivityMasterDB;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.classifications.ClassificationDataConcept;
import com.guicedee.activitymaster.core.db.entities.enterprise.Enterprise;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.implementations.ClassificationService;
import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.IProgressable;
import com.guicedee.activitymaster.core.services.classifications.enterprise.IEnterpriseName;
import com.guicedee.activitymaster.core.services.dto.*;
import com.guicedee.activitymaster.core.services.system.IClassificationDataConceptService;
import com.guicedee.activitymaster.core.services.system.IClassificationService;
import com.guicedee.activitymaster.core.services.system.IEnterpriseService;
import com.guicedee.activitymaster.core.services.system.ISystemsService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.*;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedpersistence.db.annotations.Transactional;
import geodata.GeoDataFinder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.validation.constraints.NotNull;
import java.util.*;

import static com.entityassist.querybuilder.EntityAssistStrings.*;
import static com.guicedee.activitymaster.core.services.classifications.involvedparty.InvolvedPartyClassifications.Languages;
import static com.guicedee.activitymaster.core.services.classifications.involvedparty.InvolvedPartyClassifications.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;
import static geodata.GeoDataFiles.*;

@Singleton
public class GeographyService<J extends GeographyService<J>>
		implements IGeographyService<J>, IProgressable
{
	@CacheResult(cacheName = "GeographyPlanets",
			skipGet = true)
	public IGeography<?> createPlanet(ISystems<?> originatingSystem, @CacheKey @NotNull String value, String originalUniqueID, UUID... identifyingToken)
	{
		Geography geo = new Geography();
		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(originatingSystem.getEnterpriseID());
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(activityMasterSystem.getEnterpriseID());

		Classification classification = (Classification) classificationService.find(Planet, originatingSystem.getEnterprise(), identifyingToken);

		Optional<Geography> geographyExists = geo.builder()
		                                         .withClassification(classification, null)
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .findByName(value)
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		if (geographyExists.isEmpty())
		{
			geo.setEnterpriseID(classification.getEnterpriseID());
			geo.setClassification(classification);
			geo.setSystemID((Systems) geoSystem);
			geo.setOriginalSourceSystemID((Systems) geoSystem);
			geo.setName(value);
			geo.setDescription("The Planet " + value);
			if (originalUniqueID != null)
			{
				geo.setOriginalSourceSystemUniqueID(originalUniqueID);
			}
			geo.setActiveFlagID(classification.getActiveFlagID());
			geo.persist();
			if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
			{
				geo.createDefaultSecurity(geoSystem, identifyingToken);
			}
		}
		else
		{
			geo = geographyExists.get();
		}
		return geo;
	}

	@CacheResult(cacheName = "GeographyContinents")
	public IGeography<?> createContinent(
			@CacheKey String planet, @CacheKey GeographyContinent continent, ISystems<?> originatingSystem, String originalUniqueID, UUID... identifyingToken)
	{
		Geography geo = new Geography();
		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(originatingSystem.getEnterpriseID());
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(activityMasterSystem.getEnterpriseID());

		Geography planetGeo = (Geography) findPlanet(planet, originatingSystem, identifyingToken);

		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Continent, originatingSystem.getEnterprise(), identifyingToken);

		Optional<Geography> geographyExists = geo.builder()
		                                         .withClassification(classification, null)
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .withParent(planetGeo, null)
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		if (geographyExists.isEmpty())
		{
			geo.setEnterpriseID(classification.getEnterpriseID());
			geo.setClassification(classification);
			geo.setSystemID((Systems) geoSystem);
			geo.setOriginalSourceSystemID((Systems) geoSystem);
			geo.setName(continent.getContinentCode());
			geo.setDescription(continent.getContinentName());
			if (originalUniqueID != null)
			{
				geo.setOriginalSourceSystemUniqueID(originalUniqueID);
			}
			geo.setActiveFlagID(classification.getActiveFlagID());
			geo.persist();
			if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
			{
				geo.createDefaultSecurity(geoSystem, identifyingToken);
			}
			geo.add(ContinentCode, continent.getContinentCode(), originatingSystem, identifyingToken);
			planetGeo.addChild(geo, originatingSystem.getEnterprise(), identifyingToken);
		}
		else
		{
			geo = geographyExists.get();
		}
		return geo;
	}

	@Override
	@CacheResult(cacheName = "GeographyPlanets")
	public IGeography<?> findPlanet(@CacheKey String name, ISystems<?> originatingSystem, UUID... identifyingToken)
	{
		Geography geo = new Geography();
		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(originatingSystem.getEnterpriseID());
		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Planet, originatingSystem.getEnterprise(), identifyingToken);

		Optional<Geography> geographyExists = geo.builder()
		                                         .withClassification(classification, null)
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .findByName(name)
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		return geographyExists.orElseThrow(() -> new GeographyException("Planet does not exist - " + name));
	}

	@Override
	@CacheResult(cacheName = "GeographyContinents")
	public GeographyContinent findContinent(@CacheKey GeographyContinent continent, ISystems<?> originatingSystem, UUID... identifyingToken)
	{
		Geography geo = new Geography();
		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(originatingSystem.getEnterpriseID());
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(activityMasterSystem.getEnterpriseID());

		ClassificationService classificationService = GuiceContext.get(ClassificationService.class);
		Classification classification = (Classification) classificationService.find(Continent, originatingSystem.getEnterprise(), identifyingToken);
		Classification classificationCode = (Classification) classificationService.find(ContinentCode, originatingSystem.getEnterprise(), identifyingToken);

		GeographyQueryBuilder queryBuilder = geo.builder()
		                                        .withClassification(classification, null)
		                                        .withEnterprise(originatingSystem.getEnterpriseID())
		                                        .findByName(continent.getContinentName())
		                                        .inDateRange()
		                                        .withEnterprise(activityMasterSystem.getEnterprise());

		if (continent.getContinentCode() != null)
		{
			queryBuilder.withClassification(classificationCode, continent.getContinentCode());
		}

		Optional<Geography> geographyExists = geo.builder()
		                                         .withClassification(classificationCode, continent.getContinentCode())
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .findByName(continent.getContinentCode())
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		Geography geoOut = geographyExists.orElseThrow(() -> new GeographyException("Continent does not exist - " + continent));

		return new GeographyContinent().setContinentName(geoOut.getName())
		                               .setContinentCode(geoOut
				                                                 .find(ContinentCode, geoSystem, identifyingToken)
				                                                 .orElseThrow(() -> new GeographyException(
						                                                 "Continent does not have a continent code somehow?!? - " + continent))
				                                                 .getValue());

	}

	@Override
	public void loadAsciiCodes(IEnterpriseName<?> enterpriseName, String countryCode, IActivityMasterProgressMonitor progressMonitor)
	{
		List<GeographyAsciiCode> codes = new ArrayList<>();
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
					create(ascii, enterpriseName);
				}
				if (current % 10 == 0)
				{
					logProgress("Geography Service", "Loading Ascii Admin 1 Codes", 10, progressMonitor);
				}
			}
			logProgress("Geography Service", "Finished Admin 1 Ascii Codes", 10, progressMonitor);
		}
	}

	@CacheResult(cacheName = "GeographyAdmin1AsciiCodes")
	public GeographyAsciiCode create(@CacheKey GeographyAsciiCode ascii, @CacheKey IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID token = get(GeographySystem.class).getSystemToken(enterprise);
		IClassification<?> asciiCodeParentClassification = classificationService
				                                                   .find(GeographyAdmin1AsciiCodes, enterprise, token);
		//Check for code ascii classification
		try
		{
			IClassification<?> asciiCodeClassification = classificationService.find(ascii.getName(), enterprise, token);
		}
		catch (NoSuchElementException e)
		{
			//create the classification
			IClassification<?> classy = classificationService.create(ascii.getCode(), ascii.getName(), GeoNameClassificationDataConcept,
			                                                         geoSystem,
			                                                         (short) 0,
			                                                         null,
			                                                         token);
			IClassification<?> geoNameIdClassification = classificationService
					                                             .find(GeoNameID, enterprise, token);
			IClassification<?> geoAsciiNameClassification = classificationService
					                                                .find(GeographyAsciiName, enterprise, token);

			classy.addChild(geoNameIdClassification, "" + ascii.getGeonameId(), enterprise, token);
			classy.addChild(geoAsciiNameClassification, "" + ascii.getNameAscii(), enterprise, token);
			//add it to the parent of ascii codes
			asciiCodeParentClassification.addChild(classy, STRING_EMPTY, enterprise, token);

		}
		return ascii;
	}

	@Override
	public void loadAdmin2Codes(IEnterpriseName<?> enterpriseName, String countryCode, IActivityMasterProgressMonitor progressMonitor)
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
					create(ascii, enterpriseName, true);
				}
				if (current % 10 == 0)
				{
					logProgress("Geography Service", "Loading Admin 2 Codes", 10, progressMonitor);
				}
			}
		}
		logProgress("Geography Service", "Finished Admin 2 Codes", 10, progressMonitor);
	}

	@CacheResult(cacheName = "GeographyAdmin2Codes")
	public GeographyAsciiCode create(@CacheKey GeographyAsciiCode ascii, @CacheKey IEnterpriseName<?> enterpriseName, boolean admin2Code)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID token = get(GeographySystem.class).getSystemToken(enterprise);
		IClassification<?> asciiCodeParentClassification = classificationService
				                                                   .find(GeographyAdmin2Codes, enterprise, token);
		//Check for code ascii classification
		try
		{
			IClassification<?> asciiCodeClassification = classificationService.find(ascii.getName(), enterprise, token);
		}
		catch (NoSuchElementException e)
		{
			//create the classification
			IClassification<?> classy = classificationService.create(ascii.getCode(), ascii.getName(), GeoNameClassificationDataConcept,
			                                                         geoSystem,
			                                                         (short) 0,
			                                                         null,
			                                                         token);
			IClassification<?> geoNameIdClassification = classificationService
					                                             .find(GeoNameID, enterprise, token);
			IClassification<?> geoAsciiNameClassification = classificationService
					                                                .find(GeographyAsciiName, enterprise, token);

			classy.addChild(geoNameIdClassification, "" + ascii.getGeonameId(), enterprise, token);
			classy.addChild(geoAsciiNameClassification, "" + ascii.getNameAscii(), enterprise, token);
			//add it to the parent of ascii codes
			asciiCodeParentClassification.addChild(classy, STRING_EMPTY, enterprise, token);
		}
		return ascii;
	}

	/**
	 * Found on the ascii code name field
	 *
	 * @param asciiCode
	 * @param enterpriseName
	 *
	 * @return
	 */
	@Override
	@CacheResult(cacheName = "GeographyAdmin1AsciiCodes")
	public GeographyAsciiCode findAdmin1AsciiCode(@CacheKey GeographyAsciiCode asciiCode, @CacheKey IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID token = get(GeographySystem.class).getSystemToken(enterprise);
		IClassification<?> asciiCodeClassification = classificationService.find(asciiCode.getName(), enterprise, token);
		asciiCode.setCode(asciiCodeClassification.getName());
		asciiCode.setName(asciiCodeClassification.getDescription());
		IClassification<?> geoNameIdClassification = classificationService
				                                             .find(GeoNameID, enterprise, token);
		IClassification<?> geoAsciiNameClassification = classificationService
				                                                .find(GeographyAsciiName, enterprise, token);
		asciiCode.setGeonameId(asciiCodeClassification.findLink(geoNameIdClassification, enterprise, token)
		                                              .getValueAsLong());
		asciiCode.setNameAscii(asciiCodeClassification.findLink(geoAsciiNameClassification, enterprise, token)
		                                              .getValue());
		return asciiCode;
	}

	/**
	 * Found on the ascii code name field
	 *
	 * @param asciiCode
	 * @param enterpriseName
	 *
	 * @return
	 */
	@Override
	@CacheResult(cacheName = "GeographyAdmin2Codes")
	public GeographyAsciiCode findAdmin2Code(@CacheKey GeographyAsciiCode asciiCode, @CacheKey IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID token = get(GeographySystem.class).getSystemToken(enterprise);
		IClassification<?> asciiCodeClassification = classificationService.find(asciiCode.getName(), enterprise, token);
		asciiCode.setCode(asciiCodeClassification.getName());
		asciiCode.setName(asciiCodeClassification.getDescription());
		IClassification<?> geoNameIdClassification = classificationService
				                                             .find(GeoNameID, enterprise, token);
		IClassification<?> geoAsciiNameClassification = classificationService
				                                                .find(GeographyAsciiName, enterprise, token);
		asciiCode.setGeonameId(asciiCodeClassification.findLink(geoNameIdClassification, enterprise, token)
		                                              .getValueAsLong());
		asciiCode.setNameAscii(asciiCodeClassification.findLink(geoAsciiNameClassification, enterprise, token)
		                                              .getValue());
		return asciiCode;
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

				if (language.getIso6391Code()
				            .equals("en") ||
				    language.getIso6391Code()
				            .equals("af") ||
				    language.getIso6391Code()
				            .equals("zu") &&
				    !language.getName()
				             .isEmpty())
				{
					create(language, enterpriseName);
				}
				logProgress("Geography Service", "Loading Language - " +
				                                 (language.getName()
				                                          .isEmpty() ? "-" : language.getName()
				                                                                     .toArray()[0])
						, 1, progressMonitor);

			}
		}
		logProgress("Geography Service", "Geography Associated Languages queued", 1, progressMonitor);
	}

	/**
	 * Created on the language iso 6392 code
	 *
	 * @param language
	 * @param enterpriseName
	 *
	 * @return
	 */
	@Override
	public ISO639Language create(ISO639Language language, IEnterpriseName<?> enterpriseName)
	{
		if (language.getIso6392Code()
		            .isEmpty() ||
		    language.getName()
		            .isEmpty())
		{
			return language;
		}
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		IClassification<?> languageClassification = classificationService.find(Languages, enterprise, identityToken);
		IClassification<?> iso_1 = classificationService.find(ISO639_1, enterprise, identityToken);
		IClassification<?> iso_2 = classificationService.find(ISO639_2, enterprise, identityToken);

		//Check for code ascii classification
		try
		{
			IClassification<?> asciiCodeClassification = classificationService.find(language.getIso6392Code(), enterprise, identityToken);
		}
		catch (NoSuchElementException e)
		{
			//create the classification
			IClassification<?> classy = classificationService.create(language.getIso6392Code(), language.getName()
			                                                                                            .stream()
			                                                                                            .findFirst()
			                                                                                            .get(),
			                                                         GeoNameClassificationDataConcept,
			                                                         geoSystem,
			                                                         (short) 0,
			                                                         null,
			                                                         identityToken);
			iso_2.addChild(classy, enterprise, identityToken);

			boolean skip1 = false;
			if (language.getIso6391Code()
			            .isEmpty())
			{
				skip1 = true;
			}
			IClassification<?> classy_2 = null;
			if (!skip1)
			{
				classy_2 = classificationService.create(language.getIso6391Code(), language.getName()
				                                                                           .stream()
				                                                                           .findFirst()
				                                                                           .get(),
				                                        GeoNameClassificationDataConcept,
				                                        geoSystem,
				                                        (short) 0,
				                                        null,
				                                        identityToken);
				iso_1.addChild(classy_2, enterprise, identityToken);
			}
			IClassification<?> englishNameClassification = classificationService
					                                               .find(ISO6392EnglishName, enterprise, identityToken);
			IClassification<?> frenchNameClassification = classificationService
					                                              .find(ISO6392FrenchName, enterprise, identityToken);
			IClassification<?> germanNameClassification = classificationService
					                                              .find(ISO6392GermanName, enterprise, identityToken);
			for (String s : language.getName())
			{
				classy.addChild(englishNameClassification, s, enterprise, identityToken);
				if (!skip1)
				{
					classy_2.addChild(englishNameClassification, s, enterprise, identityToken);
				}
			}
			for (String s : language.getFrenchName())
			{
				classy.addChild(frenchNameClassification, s, enterprise, identityToken);
				if (!skip1)
				{
					classy_2.addChild(frenchNameClassification, s, enterprise, identityToken);
				}
			}
			for (String s : language.getGermanName())
			{
				classy.addChild(germanNameClassification, s, enterprise, identityToken);
				if (!skip1)
				{
					classy_2.addChild(germanNameClassification, s, enterprise, identityToken);
				}
			}
		}
		return language;
	}

	/**
	 * Like en, or eng for english.. checks both
	 *
	 * @param asciiCode
	 *
	 * @return
	 */
	@Override
	@CacheResult(cacheName = "GeoFindLanguage")
	public ISO639Language findLanguage(@CacheKey String asciiCode, @CacheKey IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		//ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		//IClassification<?> languageClassification = classificationService.find(Languages, enterprise, identityToken);
		//IClassification<?> iso_1 = classificationService.find(ISO639_1, enterprise, identityToken);
		//IClassification<?> iso_2 = classificationService.find(ISO639_2, enterprise, identityToken);
		ClassificationDataConcept concept = (ClassificationDataConcept) conceptService.find(GeoNameClassificationDataConcept, enterprise, identityToken);
		Optional<Classification> classification = new Classification().builder()
		                                                              .findByNameAndConcept(asciiCode, concept, enterprise)
		                                                              .setReturnFirst(true)
		                                                              .get();
		if (classification.isEmpty())
		{
			return null;
		}

		ISO639Language language = new ISO639Language();
		language.setIso6392Code(classification.get()
		                                      .getName());
		if (language.getIso6392Code()
		            .length() == 2)
		{
			language.setIso6391Code(language.getIso6392Code());
		}

		IClassification<?> englishNameClassification = classificationService
				                                               .find(ISO6392EnglishName, enterprise, identityToken);
		IClassification<?> frenchNameClassification = classificationService
				                                              .find(ISO6392FrenchName, enterprise, identityToken);
		IClassification<?> germanNameClassification = classificationService
				                                              .find(ISO6392GermanName, enterprise, identityToken);

		language.getName()
		        .add(classification.get()
		                           .getDescription());

		var children = classification.get()
		                             .findChildren(englishNameClassification, enterpriseName, identityToken);
		for (IRelationshipValue<IClassification<?>, IClassification<?>, ?> child : children)
		{
			language.getName()
			        .add(child.getValue());
		}
		children = classification.get()
		                         .findChildren(frenchNameClassification, enterpriseName, identityToken);
		for (IRelationshipValue<IClassification<?>, IClassification<?>, ?> child : children)
		{
			language.getFrenchName()
			        .add(child.getValue());
		}
		children = classification.get()
		                         .findChildren(germanNameClassification, enterpriseName, identityToken);
		for (IRelationshipValue<IClassification<?>, IClassification<?>, ?> child : children)
		{
			language.getGermanName()
			        .add(child.getValue());
		}
		return language;
	}

	@Override
	public void loadCountryInfo(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
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

				GeographyCurrency gcc = new GeographyCurrency().setCurrencyCode(record.get(10))
				                                               .setCurrencyName(record.get(11));
				gcc = findOrCreateCurrency(gcc, enterpriseName, identityToken);
				country.setCurrency(gcc);
				country.setCountryDialCode(record.get(12));
				country.setPostalCodeDecimalFormat(record.get(13));
				country.setPostalCodeRegexFormat(record.get(14));

				String languagesList = record.get(15);
				for (String s : languagesList.split(","))
				{
					ISO639Language lang = findLanguage(s, enterpriseName);
					if (lang != null)
					{
						country.getLanguages()
						       .add(lang);
					}
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
				create(country, enterpriseName);
				logProgress("Geography Service", "Loaded Country " + country.getCountryName(), 1, progressMonitor);
			}
		}
		logProgress("Geography Service", "Finished Loading Countries", 10, progressMonitor);
	}

	@Override
	@CacheResult(cacheName = "GeographyCurrency")
	public GeographyCurrency findOrCreateCurrency(@CacheKey GeographyCurrency currency, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identifyingToken)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);

		if (currency.getCurrencyCode()
		            .isEmpty())
		{
			return currency;
		}

		ClassificationDataConcept currencyConcept = (ClassificationDataConcept) conceptService.find(GeographyCurrencyConcept, enterprise, identityToken);

		Optional<Classification> classification = new Classification().builder()
		                                                              .findByNameAndConcept(currency.getCurrencyCode(), currencyConcept, enterprise)
		                                                              .inActiveRange(enterprise, identityToken)
		                                                              .inDateRange()
		                                                              .setReturnFirst(true)
		                                                              .get();

		if (classification.isEmpty())
		{
			IClassification<?> c = classificationService.create(currency.getCurrencyCode(), currency.getCurrencyName(),
			                                                    GeographyCurrencyConcept,
			                                                    geoSystem, (short) 0, null, identityToken);
		}
		return currency;
	}

	/**
	 * Find a country on the given country code (iso)
	 *
	 * @param country
	 * @param enterpriseName
	 * @param identityToken
	 *
	 * @return
	 */
	@Override
	@CacheResult(cacheName = "GeographyCountries")
	public GeographyCountry findCountry(@CacheKey GeographyCountry country, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		IClassification<?> classification = classificationService.find(Country, enterprise, identityToken);

		Geography geo = new Geography().builder()
		                               .findByName(country.getIso())
		                               .withClassification((Classification) classification)
		                               .inActiveRange(enterprise, identityToken)
		                               .inDateRange()
		                               .get()
		                               .orElseThrow(() -> new GeographyException("Unable to find a country with the given code " + country.getIso()));

		country.setIso(geo.find(CountryISO3166, geoSystem, identityToken)
		                  .get()
		                  .getValue());
		country.setIso3(geo.find(CountryISO3166_3, geoSystem, identityToken)
		                   .get()
		                   .getValue());
		country.setIsoNumeric(geo.find(CountryISO_Numeric, geoSystem, identityToken)
		                         .get()
		                         .getValue());
		country.setFips(geo.find(CountryFips, geoSystem, identityToken)
		                   .get()
		                   .getValue());
		country.setCapital(geo.find(CountryCapital, geoSystem, identityToken)
		                      .get()
		                      .getValue());
		country.setAreaSqlKM(geo.find(CountryAreaInSqKm, geoSystem, identityToken)
		                        .get()
		                        .getValue());
		country.setWebTld(geo.find(CountryTld, geoSystem, identityToken)
		                     .get()
		                     .getValue());
		if (geo.has(Currency, geoSystem, identityToken))
		{
			String currency = geo.find(Currency, geoSystem, identityToken)
			                     .get()
			                     .getValue();
			GeographyCurrency gc = findOrCreateCurrency(new GeographyCurrency().setCurrencyCode(currency), enterpriseName);
			country.setCurrency(gc);
		}
		country.setCountryDialCode(geo.find(CountryPhone, geoSystem, identityToken)
		                              .get()
		                              .getValue());
		country.setPostalCodeDecimalFormat(geo.find(CountryPostalCodeFormat, geoSystem, identityToken)
		                                      .get()
		                                      .getValue());
		country.setPostalCodeRegexFormat(geo.find(CountryPostalCodeRegex, geoSystem, identityToken)
		                                    .get()
		                                    .getValue());
		country.setGeographyId(geo.getId());
		if (!geo.getOriginalSourceSystemUniqueID()
		        .isEmpty())
		{
			country.setGeonameId(Long.parseLong(geo.getOriginalSourceSystemUniqueID()));
		}
		return country;
	}

	/**
	 * By timezone ID
	 * <p>
	 * like Asia/Baghdad
	 *
	 * @param timezone
	 * @param enterpriseName
	 *
	 * @return
	 */
	@Override
	@CacheResult(cacheName = "GeographyTimezones")
	public GeographyTimezone findTimezone(@CacheKey GeographyTimezone timezone, @CacheKey IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);

		IClassification<?> timezoneClassification = classificationService.find(TimeZone, enterprise, identityToken);

		Geography tz = new Geography().builder()
		                              .withClassification((Classification) timezoneClassification)
		                              .inDateRange()
		                              .inActiveRange(enterprise, identityToken)
		                              .findByName(timezone.getTimezoneID())
		                              .get()
		                              .orElseThrow(() -> new GeographyException("Unable to find timezone with name - " + timezone.getTimezoneID()));

		timezone.setGeographyId(tz.getId());
		timezone.setRawOffset(tz.find(TimeZoneRawOffset, geoSystem, identityToken)
		                        .orElseThrow(() -> new GeographyException("Unable to find raw offset for timezone - what didn't load?"))
		                        .getValueAsDouble());
		timezone.setOffsetJuly2016(tz.find(TimeZoneOffsetJuly2016, geoSystem, identityToken)
		                             .orElseThrow(() -> new GeographyException("Unable to find raw offset for timezone - what didn't load?"))
		                             .getValueAsDouble());
		timezone.setOffsetJan2016(tz.find(TimeZoneOffsetJan2016, geoSystem, identityToken)
		                            .orElseThrow(() -> new GeographyException("Unable to find raw offset for timezone - what didn't load?"))
		                            .getValueAsDouble());

		return timezone;
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
				GeographyTimezone timezone = new GeographyTimezone();
				timezone.setCountryCode(record.get(0));
				timezone.setTimezoneID(record.get(1));
				timezone.setOffsetJan2016(Double.parseDouble(record.get(2)));
				timezone.setOffsetJuly2016(Double.parseDouble(record.get(3)));
				timezone.setRawOffset(Double.parseDouble(record.get(4)));
				create(timezone, enterpriseName);
				logProgress("TimeZones", "Loaded Timezone - " + timezone.getTimezoneID(), 1, progressMonitor);
			}
		}
	}

	public GeographyTimezone create(GeographyTimezone timezone, IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);

		IClassification<?> timezoneClassification = classificationService.find(TimeZone, enterprise, identityToken);

		Optional<Geography> timezoneOptional = new Geography().builder()
		                                                      .withClassification((Classification) timezoneClassification)
		                                                      .inDateRange()
		                                                      .inActiveRange(enterprise, identityToken)
		                                                      .findByName(timezone.getTimezoneID())
		                                                      .get();
		if (timezoneOptional.isPresent())
		{
			return findTimezone(timezone, enterpriseName);
		}

		Geography geo = new Geography();
		geo.setEnterpriseID((Enterprise) enterprise);
		geo.setClassification((Classification) timezoneClassification);
		geo.setSystemID((Systems) geoSystem);
		geo.setOriginalSourceSystemID((Systems) geoSystem);
		geo.setName(timezone.getTimezoneID());
		geo.setDescription(timezone.getTimezoneID());
		geo.setActiveFlagID(((Classification) timezoneClassification).getActiveFlagID());
		geo.persist();
		if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
		{
			geo.createDefaultSecurity(geoSystem, identityToken);
		}

		geo.add(TimeZoneOffsetJan2016, Double.toString(timezone.getOffsetJan2016()), geoSystem, identityToken);
		geo.add(TimeZoneOffsetJuly2016, Double.toString(timezone.getOffsetJuly2016()), geoSystem, identityToken);
		geo.add(TimeZoneRawOffset, Double.toString(timezone.getRawOffset()), geoSystem, identityToken);

		GeographyCountry gc = findCountry(new GeographyCountry().setIso(timezone.getCountryCode()), enterpriseName, identityToken);
		Geography countryGeo = new Geography()
				                       .builder()
				                       .find(gc.getGeographyId())
				                       .get()
				                       .orElseThrow(() -> new GeographyException("Unable to find a country that has loaded?"));
		countryGeo.addChild(geo, enterprise, identityToken);

		return timezone;
	}

	@Override
	public void loadPostalCodes(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		progressMonitor.setTotalTasks(3921);
		try (GeoDataFinder finder = new GeoDataFinder(ZAPostalCodes, CSVFormat.TDF, ZAPostalCodes.getHeaderNames()))
		{
			int current = 0;
			for (CSVRecord record : finder.getRecords())
			{
				GeographyPostalCode post = new GeographyPostalCode();
				String countryCode = record.get(0);
				GeographyCountry cunt = findCountry(new GeographyCountry().setIso(countryCode), enterpriseName, identityToken);
				post.setCountryCode(cunt);
				post.setPostalCode(record.get(1));
				post.setPostalCodePlaceName(record.get(2));
				GeographyCoordinates coordinates = new GeographyCoordinates(record.get(9), record.get(10));
				post.setCoordinates(coordinates);
				create(post, enterpriseName);
				logProgress("Postal Codes", "Loaded PostalCode - " + post.getPostalCode(), 1, progressMonitor);
			}
		}
	}

	/**
	 * Created with everything populated,
	 *
	 * @param postalCode
	 * @param enterpriseName
	 * @param identityToken
	 */
	public GeographyPostalCode create(GeographyPostalCode postalCode, IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);

		IClassification<?> classification = classificationService.find(PostalCode, enterprise, identityToken);

		Optional<Geography> geoSearch = new Geography().builder()
		                                               .findByName(postalCode.getPostalCode())
		                                               .withClassification((Classification) classification)
		                                               .inActiveRange(enterprise, identityToken)
		                                               .inDateRange()
		                                               .get();
		Geography geo = new Geography();
		if (geoSearch.isPresent())
		{
			geo = geoSearch.get();
		}
		else
		{
			geo.setEnterpriseID((Enterprise) enterprise);
			geo.setClassification((Classification) classification);
			geo.setSystemID((Systems) geoSystem);
			geo.setOriginalSourceSystemID((Systems) geoSystem);
			geo.setName(postalCode.getPostalCode());
			geo.setDescription(postalCode.getPostalCodePlaceName());
			geo.setActiveFlagID(((Classification) classification).getActiveFlagID());
			geo.persist();
			if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
			{
				geo.createDefaultSecurity(geoSystem, identityToken);
			}
			geo.add(Latitude, postalCode.getCoordinates()
			                            .getLatitude(), geoSystem, identityToken);
			geo.add(Longitude, postalCode.getCoordinates()
			                             .getLongitude(), geoSystem, identityToken);

			Geography country = new Geography().builder()
			                                   .find(findCountry(postalCode.getCountryCode(), enterpriseName, identityToken).getGeographyId())
			                                   .get()
			                                   .orElseThrow(() -> new GeographyException("Unable to find a country for the postal code"));
			country.add(PostalCode, geo.getName(), geoSystem, identityToken);
		}
		postalCode.setGeographyId(geo.getId());
		return postalCode;
	}

	@Override
	@CacheResult(cacheName = "GeographyPostalCodes")
	public GeographyPostalCode find(@CacheKey GeographyPostalCode postalCode, @CacheKey IEnterpriseName<?> enterpriseName, UUID... identityToken)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);

		IClassification<?> classification = classificationService.find(PostalCode, enterprise, identityToken);

		Geography geo = new Geography().builder()
		                               .findByName(postalCode.getPostalCode() + "-" + postalCode.getPostalCodePlaceName())
		                               .withClassification((Classification) classification)
		                               .inActiveRange(enterprise, identityToken)
		                               .inDateRange()
		                               .get()
		                               .orElseThrow(() -> new GeographyException("Cannot found postal code - " + postalCode.getPostalCode()));
		postalCode.setGeographyId(geo.getId());
		postalCode.setPostalCodePlaceName(geo.getDescription());
		String latitude = geo.find(Latitude, geoSystem, identityToken)
		                     .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
		                     .getValue();
		String longitude = geo.find(Longitude, geoSystem, identityToken)
		                      .orElseThrow(() -> new GeographyException("Postal Code loaded without latitude"))
		                      .getValue();
		postalCode.setCoordinates(new GeographyCoordinates(latitude, longitude));

		IClassification<?> countryClass = classificationService.find(Country, enterprise, identityToken);
		Optional<Geography> geography = new Geography().builder()
		                                               .withClassification(countryClass)
		                                               .inActiveRange(enterprise, identityToken)
		                                               .inDateRange()
		                                               .withClassification(classification, postalCode.getPostalCode())
		                                               .get();
		if (geography.isPresent())
		{
			postalCode.setCountryCode(findCountry(new GeographyCountry().setIso(geography.orElseThrow(() -> new GeographyException("Cannot find postal code in country"))
			                                                                             .getName()), enterpriseName, identityToken));
		}
		return postalCode;
	}

	@Transactional(entityManagerAnnotation = ActivityMasterDB.class,
			rollbackOn = GeographyException.class)
	public GeographyCountry create(GeographyCountry country, IEnterpriseName<?> enterpriseName)
	{
		IEnterprise<?> enterprise = GuiceContext.get(IEnterpriseService.class)
		                                        .getEnterprise(enterpriseName);
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		IClassificationDataConceptService<?> conceptService = GuiceContext.get(IClassificationDataConceptService.class);
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);

		IClassification<?> classification = classificationService.find(Country, enterprise, identityToken);

		Optional<Geography> geoSearch = new Geography().builder()
		                                               .findByName(country.getIso())
		                                               .withClassification((Classification) classification)
		                                               .inActiveRange(enterprise, identityToken)
		                                               .inDateRange()
		                                               .get();
		if (geoSearch.isPresent())
		{
			return findCountry(country, enterpriseName, identityToken);
		}

		Geography geo = new Geography();
		geo.setEnterpriseID((Enterprise) enterprise);
		geo.setClassification((Classification) classification);
		geo.setSystemID((Systems) geoSystem);
		geo.setOriginalSourceSystemID((Systems) geoSystem);
		geo.setName(country.getIso());
		geo.setDescription(country.getCountryName());
		if (country.getGeonameId() != null)
		{
			geo.setOriginalSourceSystemUniqueID(Long.toString(country.getGeonameId()));
		}
		geo.setActiveFlagID(((Classification) classification).getActiveFlagID());
		geo.persist();
		if (get(ActivityMasterConfiguration.class).isSecurityEnabled())
		{
			geo.createDefaultSecurity(geoSystem, identityToken);
		}
		//Get continent for parent
		IClassification<?> continent = classificationService.find(Continent, enterprise, identityToken);
		Geography continentGeo = new Geography().builder()
		                                        .findByName(country.getContinent()
		                                                           .getContinentCode())
		                                        .withClassification((Classification) continent)
		                                        .inActiveRange(enterprise, identityToken)
		                                        //   .inDateRange()
		                                        .get()
		                                        .orElseThrow(() -> new GeographyException("Unable to find continent in geography with : " +
		                                                                                  country.getContinent()
		                                                                                         .getContinentCode()));
		continentGeo.addChild(geo, geoSystem.getEnterprise(), identityToken);

		//Then add all the classifications for a country
		geo.add(CountryISO3166, country.getIso(), geoSystem, identityToken);
		geo.add(CountryISO3166_3, country.getIso3(), geoSystem, identityToken);
		geo.add(CountryISO_Numeric, country.getIsoNumeric(), geoSystem, identityToken);
		geo.add(CountryFips, country.getFips(), geoSystem, identityToken);
		geo.add(CountryCapital, country.getCapital(), geoSystem, identityToken);
		geo.add(CountryAreaInSqKm, country.getAreaSqlKM(), geoSystem, identityToken);
		geo.add(CountryTld, country.getWebTld(), geoSystem, identityToken);
		geo.add(CountryPhone, country.getCountryDialCode(), geoSystem, identityToken);
		geo.add(CountryPostalCodeFormat, country.getPostalCodeDecimalFormat(), geoSystem, identityToken);
		geo.add(CountryPostalCodeRegex, country.getPostalCodeRegexFormat(), geoSystem, identityToken);
		//attach currency
		ClassificationDataConcept currencyConcept = (ClassificationDataConcept) conceptService.find(GeographyCurrencyConcept, enterprise, identityToken);

		Optional<Classification> classificationCurrency = new Classification().builder()
		                                                                      .findByNameAndConcept(country.getCurrency()
		                                                                                                   .getCurrencyCode(), currencyConcept, enterprise)
		                                                                      .inActiveRange(enterprise, identityToken)
		                                                                      .inDateRange()
		                                                                      .setReturnFirst(true)
		                                                                      .get();
		classificationCurrency.ifPresent(value -> geo.add(Currency, country.getCurrency()
		                                                                   .getCurrencyCode(), geoSystem, identityToken));

		//add languages
		if (!country.getLanguages()
		            .isEmpty())
		{
			ClassificationDataConcept concept = (ClassificationDataConcept) conceptService.find(GeoNameClassificationDataConcept, enterprise, identityToken);
			for (ISO639Language language : country.getLanguages())
			{

				Optional<Classification> classificationLanguage = new Classification().builder()
				                                                                      .findByNameAndConcept(language.getIso6392Code(), concept, enterprise)
				                                                                      .setReturnFirst(true)
				                                                                      .get();
				classificationLanguage.ifPresent(value -> geo.add(value, language.getName()
				                                                                 .iterator()
				                                                                 .next(), geoSystem, identityToken));
			}
		}
		country.setGeographyId(geo.getId());
		country.setGeonameId(Long.parseLong(geo.getOriginalSourceSystemUniqueID()));
		return country;
	}
}
