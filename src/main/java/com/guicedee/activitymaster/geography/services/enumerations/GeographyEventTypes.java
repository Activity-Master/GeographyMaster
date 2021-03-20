package com.guicedee.activitymaster.geography.services.enumerations;

public enum GeographyEventTypes
{

	;

	private String description;

	GeographyEventTypes(String description)
	{
		this.description = description;
	}
	
	public String classificationValue()
	{
		return description;
	}
}
