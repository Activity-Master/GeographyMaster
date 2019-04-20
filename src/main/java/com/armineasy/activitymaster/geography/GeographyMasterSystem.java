package com.armineasy.activitymaster.geography;

import com.armineasy.activitymaster.activitymaster.db.entities.enterprise.Enterprise;
import com.armineasy.activitymaster.activitymaster.services.IActivityMasterProgressMonitor;
import com.armineasy.activitymaster.activitymaster.services.IActivityMasterSystem;
import com.armineasy.activitymaster.activitymaster.services.system.ISystemsService;
import com.google.inject.Singleton;
import com.jwebmp.guicedinjection.GuiceContext;


@Singleton
public class GeographyMasterSystem
		implements IActivityMasterSystem<GeographyMasterSystem>
{

	@Override
	public void createDefaults(Enterprise enterprise, IActivityMasterProgressMonitor progressMonitor)
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
