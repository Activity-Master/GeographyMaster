package com.guicedee.activitymaster.geography.services.enumerations;

public enum GeographyIPIdentificationTypes
{
	ISP("Internet Service Provider"),
	;
	private String classificationValue;

	GeographyIPIdentificationTypes(String classificationValue)
	{
		this.classificationValue = classificationValue;
	}

	public String classificationValue()
	{
		return name();
	}

	public String classificationDescription()
	{
		return classificationValue;
	}
}
