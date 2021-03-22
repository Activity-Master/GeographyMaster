package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of="timezoneID",callSuper = false)
public class GeographyTimezone
		extends GeographyDefaultDto<GeographyTimezone>
{
	@Serial
	private static final long serialVersionUID = 1L;

	private GeographyCountry countryCode;

	@NotNull
	private String timezoneID;

	private double offsetJan2016;
	private double offsetJuly2016;
	private double rawOffset;
}
