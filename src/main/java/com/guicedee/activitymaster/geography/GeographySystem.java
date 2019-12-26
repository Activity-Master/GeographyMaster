package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.db.entities.systems.Systems;
import com.guicedee.activitymaster.core.implementations.SystemsService;
import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.IActivityMasterSystem;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.IInvolvedPartyIdentificationType;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.system.IClassificationService;
import com.guicedee.activitymaster.core.services.system.IInvolvedPartyService;
import com.guicedee.activitymaster.core.services.system.ISystemsService;
import com.google.inject.Singleton;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeographyContinent;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyIPIdentificationTypes;
import com.guicedee.guicedinjection.GuiceContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassifications.*;
import static com.guicedee.guicedinjection.GuiceContext.*;

@Singleton
public class GeographySystem
		implements IActivityMasterSystem<GeographySystem>
{
	private static final Map<IEnterprise<?>, UUID> systemTokens = new HashMap<>();
	private static final Map<IEnterprise<?>, ISystems<?>> systemsMap = new HashMap<>();

	@Override
	public void createDefaults(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{

	}

	public void loadGeographyStatics(IEnterprise<?> enterprise)
	{

	}

	@Override
	public int totalTasks()
	{
		return 0;
	}

	@Override
	public void postStartup(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{

		final String systemName = "Geography System";
		final String systemDesc = "The system for maintaining Geography and Locations";
		Systems sys = (Systems) GuiceContext.get(SystemsService.class)
		                                    .findSystem(enterprise, systemName);
		UUID securityToken = null;
		if (sys == null)
		{
			sys = (Systems) GuiceContext.get(SystemsService.class)
			                            .create(enterprise, systemName, systemDesc, systemName);
			securityToken = GuiceContext.get(ISystemsService.class)
			                            .registerNewSystem(enterprise, sys);
		}
		else
		{
			securityToken = GuiceContext.get(SystemsService.class)
			                            .getSecurityIdentityToken(sys);
		}
		systemTokens.put(enterprise, securityToken);
		systemsMap.put(enterprise, sys);

		systemsMap.put(enterprise, get(ISystemsService.class)
				                           .create(enterprise, "Geography System",
				                                   "The system for managing Geographies and Locations", ""));
		UUID uuid = get(ISystemsService.class)
				            .registerNewSystem(enterprise, systemsMap.get(enterprise));
		systemTokens.put(enterprise, uuid);
	}

	@Override
	public void loadUpdates(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{

		IClassificationService<?> classificationService = get(IClassificationService.class);

		ISystems<?> activityMasterSystem = get(ISystemsService.class)
				                                   .getActivityMaster(enterprise);

		try
		{
			classificationService.find(Planet, enterprise, getSystemTokens().get(enterprise));
		}
		catch (Exception e)
		{

			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Geography Master", "Creating Regional Areas");
			}

			classificationService.create(Planet, systemsMap.get(enterprise));
			classificationService.create(Continent, systemsMap.get(enterprise), Planet);
			classificationService.create(Country, systemsMap.get(enterprise), Continent);
			classificationService.create(Currency, systemsMap.get(enterprise), Country);
			classificationService.create(Province, systemsMap.get(enterprise), Country);
			classificationService.create(PostalCode, systemsMap.get(enterprise), Province);
			classificationService.create(Municipalities, systemsMap.get(enterprise), Province);
			classificationService.create(City, systemsMap.get(enterprise), Municipalities);
			classificationService.create(Town, systemsMap.get(enterprise), City);
			classificationService.create(TimeZone, systemsMap.get(enterprise));

			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Geography Master", "Creating Default Geography Classifications");
			}

			classificationService.create(FeatureCodes, systemsMap.get(enterprise));
			classificationService.create(FeatureClass, systemsMap.get(enterprise));
			classificationService.create(Admin1CodeASCII, systemsMap.get(enterprise));
			classificationService.create(Admin2Code, systemsMap.get(enterprise));
			classificationService.create(Admin3Code, systemsMap.get(enterprise));
			classificationService.create(Admin4Code, systemsMap.get(enterprise));
			classificationService.create(AdminCode5, systemsMap.get(enterprise));
			classificationService.create(Population, systemsMap.get(enterprise));
			classificationService.create(Elevation, systemsMap.get(enterprise));
			classificationService.create(DEM, systemsMap.get(enterprise));
			classificationService.create(Name, systemsMap.get(enterprise));
			classificationService.create(NameAscii, systemsMap.get(enterprise));
			classificationService.create(AlternateNames, systemsMap.get(enterprise));
			classificationService.create(Latitude, systemsMap.get(enterprise));
			classificationService.create(Longitude, systemsMap.get(enterprise));
			classificationService.create(CountryCode, systemsMap.get(enterprise));
			classificationService.create(CountryCode2, systemsMap.get(enterprise));
			classificationService.create(ContinentCode, systemsMap.get(enterprise));

			//Country Data

	/*	classificationService.create(Admin1CodeASCII, newSystem.get(enterprise));
		classificationService.create(Admin1CodeASCII, newSystem.get(enterprise));
		classificationService.create(Admin1CodeASCII, newSystem.get(enterprise));
		classificationService.create(Admin1CodeASCII, newSystem.get(enterprise));
		classificationService.create(Admin1CodeASCII, newSystem.get(enterprise));
		classificationService.create(Admin1CodeASCII, newSystem.get(enterprise));
		classificationService.create(Admin1CodeASCII, newSystem.get(enterprise));
		classificationService.create(Admin1CodeASCII, newSystem.get(enterprise));
		*/
			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Geography Master", "Creating Geography Involved Parties");
			}
			//Create Identification TYpe
			IInvolvedPartyService<?> involvedPartyService = get(IInvolvedPartyService.class);
			IInvolvedPartyIdentificationType<?> idType = involvedPartyService
					                                             .createIdentificationType(enterprise, GeographyIPIdentificationTypes.ISP,
					                                                                       "An Internet Service Provider",
					                                                                       systemTokens.get(enterprise));
			idType.createDefaultSecurity(activityMasterSystem);

			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Loading Geography Updates", "Creating Planets");
			}
			//Create Planets and Continents by default
			GeographyService<?> service = (GeographyService<?>) get(IGeographyService.class);
			service.createPlanet(getSystemsMap().get(enterprise), "Earth", null, getSystemTokens().get(enterprise));
			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Loading Geography Updates", "Creating Continents");
			}

			service.createContinent("Earth", new GeographyContinent().setContinentName("Africa")
			                                                         .setContinentCode("AF"), getSystemsMap().get(enterprise), "6255146", getSystemTokens().get(enterprise));
			service.createContinent("Earth", new GeographyContinent().setContinentName("Asia")
			                                                         .setContinentCode("AS"), getSystemsMap().get(enterprise), "6255147", getSystemTokens().get(enterprise));
			service.createContinent("Earth", new GeographyContinent().setContinentName("North America")
			                                                         .setContinentCode("NA"), getSystemsMap().get(enterprise), "6255149", getSystemTokens().get(enterprise));
			service.createContinent("Earth", new GeographyContinent().setContinentName("Europe")
			                                                         .setContinentCode("EU"), getSystemsMap().get(enterprise), "6255148", getSystemTokens().get(enterprise));
			service.createContinent("Earth", new GeographyContinent().setContinentName("Oceania")
			                                                         .setContinentCode("OC"), getSystemsMap().get(enterprise), "6255151", getSystemTokens().get(enterprise));
			service.createContinent("Earth", new GeographyContinent().setContinentName("South America")
			                                                         .setContinentCode("SA"), getSystemsMap().get(enterprise), "6255150", getSystemTokens().get(enterprise));
			service.createContinent("Earth", new GeographyContinent().setContinentName("Antarctica")
			                                                         .setContinentCode("AN"), getSystemsMap().get(enterprise), "6255152", getSystemTokens().get(enterprise));
			//createInvolvedPartyClassifications(enterprise);
			if (progressMonitor != null)
			{
				progressMonitor.progressUpdate("Loading Geography Updates", "Creating Feature Classes");
			}
		}
	}

	public static Map<IEnterprise<?>, UUID> getSystemTokens()
	{
		return systemTokens;
	}

	public static Map<IEnterprise<?>, ISystems<?>> getSystemsMap()
	{
		return systemsMap;
	}
}
