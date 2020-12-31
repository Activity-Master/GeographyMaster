package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import com.guicedee.activitymaster.geography.services.dto.classifications.GeographyAsciiCode;
import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class GeoNameDefaultData<J extends GeoNameDefaultData<J>>
		extends GeographyDefaultDto
		implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;
	private final List<String> alternateNames = new ArrayList<>();
	private String name;
	private String asciiname;
	private GeographyCoordinates coordinates;

	private GeographyFeatureClassesClassifications featureClass;
	private GeographyFeatureCode featureCode;
	private GeographyCountry countryCode;

	private GeographyAsciiCode admin1Code;
	private GeographyAsciiCode admin2Code;
	private String admin3Code;
	private String admin4Code;

	private int population;
	private int elevation;
	private int dem;
	@NotNull
	private GeographyTimezone timezone;

	private LocalDateTime modificationDate;
}
