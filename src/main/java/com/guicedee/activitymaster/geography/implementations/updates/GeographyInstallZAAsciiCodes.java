package com.guicedee.activitymaster.geography.implementations.updates;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.fsdm.client.services.systems.*;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Named;
import lombok.extern.log4j.Log4j2;

import static com.guicedee.activitymaster.fsdm.services.ActivityMasterSystemsManager.*;
import static com.guicedee.activitymaster.geography.services.IGeographyService.*;

@SortedUpdate(sortOrder = 1400, taskCount = 1)
@Log4j2
public class GeographyInstallZAAsciiCodes implements ISystemUpdate
{
	@Inject
	@Named(GeographySystemName)
	private Provider<ISystems<?,?>> system;

	@Inject
	private IGeographyService<?> geographyService;

	@Override
	public Uni<Boolean> update(IEnterprise<?,?> enterprise)
	{
		log.info("Starting province codes loading for Geography Master");
		return geographyService.loadProvincesASCII1(system.get(), "ZA")
			.chain(() -> {
				wipeCaches();
				return Uni.createFrom().item(true);
			})
			.onFailure().invoke(error -> log.error("Error loading province codes: {}", error.getMessage(), error));
	}

}
