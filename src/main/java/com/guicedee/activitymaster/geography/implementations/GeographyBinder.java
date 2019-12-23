package com.guicedee.activitymaster.geography.implementations;

import com.google.inject.PrivateModule;
import com.guicedee.activitymaster.geography.GeographyService;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;

public class GeographyBinder
		extends PrivateModule
		implements IGuiceModule<GeographyBinder>
{

	@Override
	protected void configure()
	{
		bind(IGeographyService.class).to(GeographyService.class);
		expose(IGeographyService.class);
	}
}
