package com.guicedee.activitymaster.geography.services.enumerations;

import com.guicedee.activitymaster.core.services.classifications.geography.IGeographyClassification;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationDataConceptValue;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationValue;

import static com.guicedee.activitymaster.core.services.concepts.EnterpriseClassificationDataConcepts.*;

public enum GeographyFeatureClassesClassifications
		implements IClassificationValue<GeographyFeatureClassesClassifications>
				           , IGeographyClassification<GeographyFeatureClassesClassifications>
{
	A("A country, state, region, etc", Geography),
	H("A stream lake, etc", Geography),
	L("Parks, Areas, etc", Geography),
	P("Cities Villagers, etc", Geography),
	R("Roads, Railroad", Geography),
	S("Spot, Building, Farm", Geography),
	T("Mountain, Hill,Rock ...", Geography),
	U("Undersea", Geography),
	F("Forest, Heath,...", Geography),
	;

	private String description;
	private IClassificationDataConceptValue<?> concept;

	GeographyFeatureClassesClassifications(String description, IClassificationDataConceptValue<?> concept)
	{
		this.description = description;
		this.concept = concept;
	}

	GeographyFeatureClassesClassifications(String description)
	{
		this.description = description;
	}

	@Override
	public String classificationDescription()
	{
		return this.description;
	}

	@Override
	public IClassificationDataConceptValue<?> concept()
	{
		return concept;
	}
}
