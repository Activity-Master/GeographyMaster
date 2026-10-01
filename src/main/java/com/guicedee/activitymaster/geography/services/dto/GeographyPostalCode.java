package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of = "postalCode",callSuper = false)
public class GeographyPostalCode
		extends GeographyDefaultDto
		implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;
	@NotNull
	private String postalCode;

	private GeographyCountry countryCode;
	private String postalCodePlaceName;

	private String parentPlaceName;
	private String provinceName;
	/** Country-qualified GeoNames admin1 key, such as AL.40. */
	private String provinceCode;

	private GeographyCoordinates coordinates;
}
