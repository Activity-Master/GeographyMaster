package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of="continentCode",callSuper = false)
public class GeographyContinent
		extends GeographyDefaultDto<GeographyContinent>
{
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
