package com.guicedee.activitymaster.geography.implementations.updates;

import com.google.inject.Inject;
import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.enterprise.IEnterprise;
import com.guicedee.activitymaster.fsdm.client.services.systems.*;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import lombok.extern.log4j.Log4j2;

import static com.guicedee.activitymaster.fsdm.services.ActivityMasterSystemsManager.*;
import static com.guicedee.activitymaster.geography.services.IGeographyService.*;

@SortedUpdate(sortOrder = 1400, taskCount = 1)
@Log4j2
public class GeographyInstallZAAsciiCodes implements ISystemUpdate
{
	@Inject
	private IGeographyService<?> geographyService;

	@Override
	public Uni<Boolean> update(Mutiny.Session session, IEnterprise<?,?> enterprise)
	{
		log.info("Starting province codes loading for Geography Master");
		return SessionUtils.<Boolean>withActivityMaster(enterprise.getName(), GeographySystemName, tuple -> {
			var amSession = tuple.getItem1();
			var amSystem = tuple.getItem3();
			var amToken = tuple.getItem4();
			return geographyService.loadProvincesASCII1(amSession, amSystem, "ZA", amToken)
				.chain(() -> {
					wipeCaches();
					return Uni.createFrom().item(true);
				});
		}).onFailure().invoke(error -> log.error("Error loading province codes: {}", error.getMessage(), error));
	}
}
