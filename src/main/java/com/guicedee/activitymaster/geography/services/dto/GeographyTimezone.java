package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Accessors(chain = true)
public class GeographyTimezone
		extends GeographyDefaultDto
{
	private static final long serialVersionUID = 1L;

	@Size(max = 2,
			min = 2)
	private String countryCode;
	@NotNull
	private String timezoneID;

	private double offsetJan2016;
	private double offsetJuly2016;
	private double rawOffset;
}
