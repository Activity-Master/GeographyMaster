package com.guicedee.activitymaster.geography.services.enumerations;

import com.guicedee.activitymaster.core.services.classifications.geography.IGeographyClassification;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationDataConceptValue;
import com.guicedee.activitymaster.core.services.enumtypes.IClassificationValue;

import static com.guicedee.activitymaster.geography.services.enumerations.GeographyClassificationDataConcepts.*;

public enum GeographyFeatureClassesClassifications
		implements IClassificationValue<GeographyFeatureClassesClassifications>
				           , IGeographyClassification<GeographyFeatureClassesClassifications>
{
	A("A country, state, region, etc", GeoNameClassificationDataConcept),
	H("A stream lake, etc", GeoNameClassificationDataConcept),
	L("Parks, Areas, etc", GeoNameClassificationDataConcept),
	P("Cities Villagers, etc", GeoNameClassificationDataConcept),
	R("Roads, Railroad", GeoNameClassificationDataConcept),
	S("Spot, Building, Farm", GeoNameClassificationDataConcept),
	T("Mountain, Hill,Rock ...", GeoNameClassificationDataConcept),
	U("Undersea", GeoNameClassificationDataConcept),
	F("Forest, Heath,...", GeoNameClassificationDataConcept),
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
