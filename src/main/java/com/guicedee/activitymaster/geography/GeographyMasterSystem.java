package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.IActivityMasterSystem;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.system.IClassificationService;
import com.guicedee.activitymaster.core.services.system.ISystemsService;
import com.google.inject.Singleton;
import com.guicedee.guicedinjection.GuiceContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class GeographyMasterSystem
		implements IActivityMasterSystem<GeographyMasterSystem>
{
	private static final Map<IEnterprise<?>, UUID> systemTokens = new HashMap<>();
	private static final Map<IEnterprise<?>, ISystems> newSystem = new HashMap<>();

	@Override
	public void createDefaults(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{
		IClassificationService<?> classificationService = GuiceContext.get(IClassificationService.class);
		ISystems activityMasterSystem = GuiceContext.get(ISystemsService.class)
		                                            .getActivityMaster(enterprise);




	}

	@Override
	public int totalTasks()
	{
		return 0;
	}


	@Override
	public void postUpdate(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{
		newSystem.put(enterprise, GuiceContext.get(ISystemsService.class)
		                                      .create(enterprise, "Geography System",
		                                              "The system for managing Geographies and Locations", ""));
		UUID uuid = GuiceContext.get(ISystemsService.class)
		                        .registerNewSystem(enterprise, newSystem.get(enterprise));
		systemTokens.put(enterprise, uuid);

		//createInvolvedPartyClassifications(enterprise);
	}

	public static Map<IEnterprise<?>, UUID> getSystemTokens()
	{
		return systemTokens;
	}

	public static Map<IEnterprise<?>, ISystems> getNewSystem()
	{
		return newSystem;
	}
}
