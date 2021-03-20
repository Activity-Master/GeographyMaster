package com.guicedee.activitymaster.geography.services.enumerations;

import com.fasterxml.jackson.annotation.JsonCreator;

import static com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts.*;


public enum GeographyFeatureClassesClassifications
{
	A("A country, state, region, etc", GeographyXClassification),
	H("A stream lake, etc", GeographyXClassification),
	L("Parks, Areas, etc", GeographyXClassification),
	P("Cities Villagers, etc", GeographyXClassification),
	R("Roads, Railroad", GeographyXClassification),
	S("Spot, Building, Farm", GeographyXClassification),
	T("Mountain, Hill,Rock ...", GeographyXClassification),
	U("Undersea", GeographyXClassification),
	V("Forest, Heath,...", GeographyXClassification),
	Z("Unknown",GeographyXClassification)
	;

	private String description;
	private com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts concept;

	GeographyFeatureClassesClassifications(String description, com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts concept)
	{
		this.description = description;
		this.concept = concept;
	}

	@JsonCreator
	public static GeographyFeatureClassesClassifications fromString(String value)
	{
		if(value == null)
			return Z;
		for (GeographyFeatureClassesClassifications gg : GeographyFeatureClassesClassifications.values()) {
			if(gg.name().equalsIgnoreCase(value.charAt(0) + ""))
			{
				return gg;
			}
		}
		return Z;
	}

	GeographyFeatureClassesClassifications(String description)
	{
		this.description = description;
	}
	
	public String classificationDescription()
	{
		return this.description;
	}
	
	public com.guicedee.activitymaster.client.services.classifications.EnterpriseClassificationDataConcepts concept()
	{
		return concept;
	}
	
	public String toString() {
		return name();
	}
}
