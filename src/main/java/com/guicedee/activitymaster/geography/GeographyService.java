package com.guicedee.activitymaster.geography;

import com.google.inject.Singleton;
import com.guicedee.activitymaster.geography.services.IGeographyService;

@Singleton
public class GeographyService<J extends GeographyService<J>>
		implements IGeographyService<J>
{

}
