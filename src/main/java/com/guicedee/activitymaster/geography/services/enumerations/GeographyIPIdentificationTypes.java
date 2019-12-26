package com.guicedee.activitymaster.geography.services.enumerations;

import com.guicedee.activitymaster.core.services.enumtypes.IIdentificationType;
import com.guicedee.activitymaster.core.services.types.IdentificationTypes;

public enum GeographyIPIdentificationTypes
		implements IIdentificationType<IdentificationTypes>
{
	ISP("Internet Service Provider"),
	;
	private String classificationValue;

	GeographyIPIdentificationTypes(String classificationValue)
	{
		this.classificationValue = classificationValue;
	}

	@Override
	public String classificationValue()
	{
		return name();
	}

	@Override
	public String classificationDescription()
	{
		return classificationValue;
	}
}
