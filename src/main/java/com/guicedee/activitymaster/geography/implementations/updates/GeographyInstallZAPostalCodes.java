package com.guicedee.activitymaster.geography.implementations.updates;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.systems.*;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import jakarta.inject.Named;

import static com.guicedee.activitymaster.geography.services.IGeographyService.*;
import static com.guicedee.activitymaster.fsdm.services.ActivityMasterSystemsManager.*;

@SortedUpdate(sortOrder = 1800, taskCount = 1)
public class GeographyInstallZAPostalCodes implements ISystemUpdate
{
	@Inject
	@Named(GeographySystemName)
	private Provider<ISystems<?,?>> system;
	
	@Inject
	private IGeographyService<?> geographyService;
	
	@Override
	public void update(IEnterprise<?,?> enterprise)
	{
		geographyService.loadPostalCodes(system.get());
		wipeCaches();
	}
}
