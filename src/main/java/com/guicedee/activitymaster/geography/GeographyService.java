package com.guicedee.activitymaster.geography;

import com.google.inject.Singleton;
import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.classifications.ClassificationDataConcept;
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
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedinjection.interfaces.JobService;
import geodata.GeoDataFinder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
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
	public IGeography<?> createPlanet(ISystems<?> originatingSystem, @CacheKey @NotNull String value, @Null String originalUniqueID, UUID... identifyingToken)
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
			@CacheKey String planet, @CacheKey GeographyContinent continent, ISystems<?> originatingSystem, @Null String originalUniqueID, UUID... identifyingToken)
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
			geo.setName(continent.getContinentName());
			geo.setDescription("The continent " + continent.getContinentName());
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
	public GeographyContinent findContinent(GeographyContinent continent, ISystems<?> originatingSystem, UUID... identifyingToken)
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
		                                         .withClassification(classification, null)
		                                         .withEnterprise(originatingSystem.getEnterpriseID())
		                                         .findByName(continent.getContinentName())
		                                         .inDateRange()
		                                         .withEnterprise(activityMasterSystem.getEnterprise())
		                                         .get();
		Geography geoOut = geographyExists.orElseThrow(() -> new GeographyException("Continent does not exist - " + continent));

		return new GeographyContinent().setContinentName(geoOut.getName())
		                               .setContinentCode(geo.find(ContinentCode, geoSystem, identifyingToken)
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
				     .setGeonameId(Integer.parseInt(record.get(3)));
				if (countryCode != null && ascii.getCode()
				                                .startsWith(countryCode.toUpperCase() + "."))
				{
					JobService.getInstance()
					          .addJob("GeographyCreateAsciiCodes", () ->
					          {
						          create(ascii, enterpriseName);
					          });
				}
				if (current % 10 == 0)
				{
					logProgress("Geography Service", "Loading Ascii Admin 1 Codes", 10, progressMonitor);
				}
			}
			progressMonitor.setTotalTasks(100);
			progressMonitor.setCurrentTask(100);
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
		progressMonitor.setTotalTasks(44200);
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
				     .setGeonameId(Integer.parseInt(record.get(3)));
				if (countryCode != null && ascii.getCode()
				                                .startsWith(countryCode.toUpperCase() + "."))
				{
					JobService.getInstance()
					          .addJob("GeographyCreateAdmin2Codes", () ->
					          {
						          create(ascii, enterpriseName, true);
					          });
				}
				if (current % 10 == 0)
				{
					logProgress("Geography Service", "Loading Admin 2 Codes", 10, progressMonitor);
				}
			}
		}
		progressMonitor.setTotalTasks(100);
		progressMonitor.setCurrentTask(100);
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
		                                              .getValueAsNumber());
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
		                                              .getValueAsNumber());
		asciiCode.setNameAscii(asciiCodeClassification.findLink(geoAsciiNameClassification, enterprise, token)
		                                              .getValue());
		return asciiCode;
	}

	@Override
	public void loadLanguages(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{
		progressMonitor.setTotalTasks(44200);
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
				JobService.getInstance()
				          .addJob("GeographyCreateLanguages", () ->
				          {
					          create(language, enterpriseName);
				          });
				if (current % 10 == 0)
				{
					logProgress("Geography Service", "Loading Geography Associated Languages", 10, progressMonitor);
				}
			}
		}
		progressMonitor.setTotalTasks(100);
		progressMonitor.setCurrentTask(100);
		logProgress("Geography Service", "Geography Associated Languages queued", 10, progressMonitor);
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
		ISystems<?> geoSystem = get(GeographySystem.class).getSystem(enterprise);
		UUID identityToken = get(GeographySystem.class).getSystemToken(enterprise);
		IClassification<?> languageClassification = classificationService.find(Languages, enterprise, identityToken);
		IClassification<?> iso_1 = classificationService.find(ISO639_1, enterprise, identityToken);
		IClassification<?> iso_2 = classificationService.find(ISO639_2, enterprise, identityToken);
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
		List<IRelationshipValue<IClassification<?>, IClassification<?>, ?>> children = classification.get()
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

	public void loadCountryInfo(IEnterpriseName<?> enterpriseName, IActivityMasterProgressMonitor progressMonitor)
	{

	}
}
