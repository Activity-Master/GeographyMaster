package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotNull;

import java.io.Serial;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of="continentCode",callSuper = false)
public class GeographyContinent
		extends GeographyDefaultDto<GeographyContinent>
{
	@Serial
	private static final long serialVersionUID = 1L;

	@NotNull
	private String continentCode;
	@NotNull
	private String continentName;

	@Override
	public String toString()
	{
		return continentName;
	}
}
