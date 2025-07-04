package com.guicedee.activitymaster.geography.implementations.updates;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.systems.*;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import io.vertx.core.Future;
import jakarta.inject.Named;

import static com.guicedee.activitymaster.fsdm.services.ActivityMasterSystemsManager.*;
import static com.guicedee.activitymaster.geography.services.IGeographyService.*;

@SortedUpdate(sortOrder = 1200, taskCount = 1)
public class GeographyInstallCountries implements ISystemUpdate
{
	@Inject
	@Named(GeographySystemName)
	private Provider<ISystems<?,?>> system;

	@Inject
	private IGeographyService<?> geographyService;

	@Override
	public Future<Boolean> update(IEnterprise<?,?> enterprise)
	{
		geographyService.loadCountryInfo(system.get());
		wipeCaches();
		return Future.succeededFuture(true);
	}

}
