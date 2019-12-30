package com.guicedee.activitymaster.geography;

import com.google.inject.Singleton;
import com.guicedee.activitymaster.core.ActivityMasterConfiguration;
import com.guicedee.activitymaster.core.db.entities.classifications.Classification;
import com.guicedee.activitymaster.core.db.entities.geography.Geography;
import com.guicedee.activitymaster.core.db.entities.geography.builders.GeographyQueryBuilder;
import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.implementations.ClassificationService;
import com.guicedee.activitymaster.core.services.classifications.enterprise.IEnterpriseName;
import com.guicedee.activitymaster.core.services.dto.IClassification;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.IGeography;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.system.IClassificationService;
import com.guicedee.activitymaster.core.services.system.IEnterpriseService;
import com.guicedee.activitymaster.core.services.system.ISystemsService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.exceptions.GeographyException;
import com.guicedee.guicedinjection.GuiceContext;
import geodata.GeoDataFinder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.entityassist.querybuilder.EntityAssistStrings.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.*;
import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;
import static geodata.GeoDataFiles.*;

@Singleton
public class GeographyService<J extends GeographyService<J>>
		implements IGeographyService<J>
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
	public List<GeographyAsciiCode> loadAsciiCodes(IEnterpriseName<?> enterpriseName)
	{
		List<GeographyAsciiCode> codes = new ArrayList<>();
		try (GeoDataFinder finder = new GeoDataFinder(Admin1CodesASCII, CSVFormat.TDF, Admin1CodesASCII.getHeaderNames()))
		{
			for (CSVRecord record : finder.getRecords())
			{
				GeographyAsciiCode ascii = new GeographyAsciiCode();
				ascii.setCode(record.get(0))
				     .setName(record.get(1))
				     .setNameAscii(record.get(2))
				     .setGeonameId(Integer.parseInt(record.get(3)));
				create(ascii, enterpriseName);
				codes.add(ascii);
			}
		}

		return codes;
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
		IClassification<?> asciiCodeClassification = classificationService.find(ascii.getName(), enterprise, token);
		if (asciiCodeClassification == null)
		{
			//create the classification
			IClassification<?> classy = classificationService.create(ascii.getCode(), ascii.getName(), GeoNameClassificationDataConcept,
			                                                         geoSystem,
			                                                         (short) 0,
			                                                         null,
			                                                         token);
			//add it to the parent of ascii codes
			asciiCodeParentClassification.addChild((Classification) classy, STRING_EMPTY, enterprise, token);
		}
		return ascii;
	}

}
