package com.guicedee.activitymaster.geography;

import com.google.inject.*;
import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.IActivityMasterSystem;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.system.ActivityMasterDefaultSystem;
import com.guicedee.activitymaster.core.services.system.ISystemsService;

import static com.guicedee.activitymaster.geography.services.IGeographyService.*;

@Singleton
public class GeographySystem
		extends ActivityMasterDefaultSystem<GeographySystem>
		implements IActivityMasterSystem<GeographySystem>
{
	@Inject
	private Provider<ISystemsService<?>> systemsService;
	
	@Override
	public void registerSystem(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{
		systemsService.get()
		              .create(enterprise, getSystemName(), getSystemDescription());
	}
	
	
	@Override
	public void createDefaults(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
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
