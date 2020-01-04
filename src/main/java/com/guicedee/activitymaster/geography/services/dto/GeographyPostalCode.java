package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Data
@Accessors(chain = true)
public class GeographyPostalCode
		extends GeographyDefaultDto
		implements Serializable
{
	private static final long serialVersionUID = 1L;
	@NotNull
	private String postalCode;

	private GeographyCountry countryCode;
	private String postalCodePlaceName;

	private GeographyCoordinates coordinates;

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		if (!super.equals(o))
		{
			return false;
		}
		GeographyPostalCode that = (GeographyPostalCode) o;
		return Objects.equals(getPostalCode(), that.getPostalCode());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), getPostalCode());
	}
}
