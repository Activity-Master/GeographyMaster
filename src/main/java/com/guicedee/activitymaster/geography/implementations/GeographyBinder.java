package com.guicedee.activitymaster.geography.implementations;

import com.google.inject.*;
import com.guicedee.activitymaster.geography.GeographyService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.client.services.lifecycle.IGuiceModule;

public class GeographyBinder
		extends AbstractModule
		implements IGuiceModule<GeographyBinder>
{
	@Override
	protected void configure()
	{
		bind(IGeographyService.class).to(GeographyService.class).in(Singleton.class);
	}
}