package com.guicedee.activitymaster.geography.services.enumerations;

import com.guicedee.activitymaster.core.services.enumtypes.IEventTypeValue;

public enum GeographyEventTypes
		implements IEventTypeValue<GeographyEventTypes>
{

	;

	private String description;

	GeographyEventTypes(String description)
	{
		this.description = description;
	}

	@Override
	public String classificationValue()
	{
		return description;
	}
}
