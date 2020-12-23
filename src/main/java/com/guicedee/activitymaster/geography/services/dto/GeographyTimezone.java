package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of="timezoneID",callSuper = false)
public class GeographyTimezone
		extends GeographyDefaultDto
{
	private static final long serialVersionUID = 1L;

	private GeographyCountry countryCode;

	@NotNull
	private String timezoneID;

	private double offsetJan2016;
	private double offsetJuly2016;
	private double rawOffset;
}
