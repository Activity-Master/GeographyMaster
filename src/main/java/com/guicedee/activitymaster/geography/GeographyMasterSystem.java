package com.guicedee.activitymaster.geography;

import com.guicedee.activitymaster.core.services.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.core.services.IActivityMasterSystem;
import com.guicedee.activitymaster.core.services.dto.IEnterprise;
import com.guicedee.activitymaster.core.services.system.ISystemsService;
import com.google.inject.Singleton;
import com.guicedee.guicedinjection.GuiceContext;


@Singleton
public class GeographyMasterSystem
		implements IActivityMasterSystem<GeographyMasterSystem>
{

	@Override
	public void createDefaults(IEnterprise<?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{

		GuiceContext.get(ISystemsService.class)
		            .getActivityMaster(enterprise);
	}

	@Override
	public int totalTasks()
	{
		return 0;
	}
}
