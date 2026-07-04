package com.guicedee.activitymaster.geography.implementations;

import com.google.inject.*;
import com.guicedee.activitymaster.geography.GeographyService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class GeographyBinder
		extends AbstractModule
		implements IGuiceModule<GeographyBinder>
{
	@Override
	protected void configure()
	{
		log.info("🌍 Using Geography Activity Master Module and routes at /rest/{enterprise}/geography");
		// Follow the canonical FSDM binder pattern so that both the wildcard
		// (IGeographyService<?>) and the concrete-generic (IGeographyService<GeographyService>)
		// injection points resolve — not just the raw IGeographyService type.
		@SuppressWarnings("Convert2Diamond")
		Key<IGeographyService<?>> geographyServiceKey = Key.get(new TypeLiteral<IGeographyService<?>>() {});
		@SuppressWarnings("Convert2Diamond")
		Key<IGeographyService<GeographyService>> geographyServiceKeyLegit = Key.get(new TypeLiteral<IGeographyService<GeographyService>>() {});

		bind(geographyServiceKeyLegit).to(GeographyService.class).in(Singleton.class);
		bind(geographyServiceKey).to(geographyServiceKeyLegit);
		bind(IGeographyService.class).to(geographyServiceKey);
	}
}