package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class GeographyCoordinates
		extends GeographyDefaultDto
		implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;

	private String latitude;
	private String longitude;

	public static GeographyCoordinates from(String latitude, String longitude)
	{
		return new GeographyCoordinates(latitude, longitude);
	}

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
		GeographyCoordinates that = (GeographyCoordinates) o;
		return Objects.equals(getLatitude(), that.getLatitude()) &&
		       Objects.equals(getLongitude(), that.getLongitude());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(getLatitude(), getLongitude());
	}
}
