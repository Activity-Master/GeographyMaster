package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class Timezone
{
	@Size(max = 2,
			min = 2)
	private String countryCode;
	@NotNull
	private String timezoneID;

	private double offsetJan2016;
	private double offsetJuly2016;
	private double rawOffset;
}
