package com.guicedee.activitymaster.geography.implementations;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.guicedee.activitymaster.client.services.ISystemsService;
import com.guicedee.activitymaster.client.services.administration.ActivityMasterDefaultSystem;
import com.guicedee.activitymaster.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.client.services.systems.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.client.services.systems.IActivityMasterSystem;

import static com.guicedee.activitymaster.geography.services.IGeographyService.*;

public class GeographySystem
		extends ActivityMasterDefaultSystem<GeographySystem>
		implements IActivityMasterSystem<GeographySystem>
{
	@Inject
	private Provider<ISystemsService<?>> systemsService;
	
	@Override
	public ISystems<?,?> registerSystem(IEnterprise<?,?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{
		ISystems<?, ?> iSystems = systemsService.get()
		                                        .create(enterprise, getSystemName(), getSystemDescription());
		systemsService.get()
		              .registerNewSystem(enterprise, getSystem(enterprise));
		return iSystems;
	}
	
	@Override
	public void createDefaults(IEnterprise<?,?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{

	}

	@Override
	public int totalTasks()
	{
		return 0;
	}

	@Override
	public String getSystemName()
	{
		return GeographySystemName;
	}

	@Override
	public String getSystemDescription()
	{
		return "The system for maintaining Geography and Locations";
	}
	
}
