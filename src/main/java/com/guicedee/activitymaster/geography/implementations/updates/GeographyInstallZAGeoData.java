package com.guicedee.activitymaster.geography.implementations.updates;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.guicedee.activitymaster.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.client.services.systems.*;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import jakarta.inject.Named;

import static com.guicedee.activitymaster.core.services.ActivityMasterSystemsManager.*;
import static com.guicedee.activitymaster.geography.services.IGeographyService.*;

@DatedUpdate(date = "2021/01/25", taskCount = 1)
public class GeographyInstallZAGeoData implements ISystemUpdate
{
	@Inject
	@Named(GeographySystemName)
	private Provider<ISystems<?,?>> system;
	
	@Inject
	private IGeographyService<?> geographyService;
	
	@Override
	public void update(IEnterprise<?,?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{
		geographyService.loadTownsAndCities(system.get(), progressMonitor);
		wipeCaches();
	}
	
}
