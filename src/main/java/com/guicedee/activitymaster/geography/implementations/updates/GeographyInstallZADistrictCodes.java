package com.guicedee.activitymaster.geography.implementations.updates;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.guicedee.activitymaster.client.services.administration.IActivityMasterProgressMonitor;
import com.guicedee.activitymaster.client.services.administration.ISystemUpdate;
import com.guicedee.activitymaster.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.core.updates.DatedUpdate;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import jakarta.inject.Named;

import static com.guicedee.activitymaster.core.services.ActivityMasterSystemsManager.*;
import static com.guicedee.activitymaster.geography.services.IGeographyService.*;

@DatedUpdate(date = "2021/01/10", taskCount = 1)
public class GeographyInstallZADistrictCodes implements ISystemUpdate
{
	@Inject
	@Named(GeographySystemName)
	private Provider<ISystems> system;
	
	@Inject
	private IGeographyService<?> geographyService;
	
	@Override
	public void update(IEnterprise<?,?> enterprise, IActivityMasterProgressMonitor progressMonitor)
	{
		geographyService.loadDistrictsASCII2(system.get(), "ZA", progressMonitor);
		wipeCaches();
	}
	
}
