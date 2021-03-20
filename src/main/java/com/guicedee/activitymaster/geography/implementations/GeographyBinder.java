package com.guicedee.activitymaster.geography.implementations;

import com.google.inject.*;
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
		@SuppressWarnings("Convert2Diamond")
		Key<IGeographyService<?>> genericKey = Key.get(new TypeLiteral<IGeographyService<?>>() {});
		@SuppressWarnings("Convert2Diamond")
		Key<IGeographyService<GeographyService>> realKey
				= Key.get(new TypeLiteral<IGeographyService<GeographyService>>() {});
		
		bind(genericKey).to(realKey);
		bind(realKey).to(GeographyService.class);
		bind(IGeographyService.class).to(genericKey);
		
		expose(genericKey);
		expose(IGeographyService.class);
	}
}
