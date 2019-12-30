package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class GeographyPostalCode
		extends GeographyDefaultDto
		implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String postalCode;
	private String postalCodeRegex;

	@Size(max = 2,
			min = 2)
	@NotNull
	private String countryCode;

	private String postalCodePlaceName;

	private GeographyCoordinates coordinates;
}
