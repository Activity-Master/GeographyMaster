package com.guicedee.activitymaster.geography.services.enumerations;

import com.guicedee.activitymaster.core.services.enumtypes.IClassificationDataConceptValue;

public enum GeographyClassificationDataConcepts
		implements IClassificationDataConceptValue<GeographyClassificationDataConcepts>
{
	GeoNameClassificationDataConcept,

	;

	private String classificationValue;

	GeographyClassificationDataConcepts()
	{
		this.classificationValue = name();
	}

	GeographyClassificationDataConcepts(String classificationValue)
	{
		this.classificationValue = classificationValue;
	}

	public static GeographyClassificationDataConcepts fromClassName(Class clazz)
	{
		return GeographyClassificationDataConcepts.valueOf(clazz.getSimpleName());
	}

	@Override
	public String classificationValue()
	{
		return classificationValue;
	}
}
