package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Data
@Accessors(chain = true)
public class GeographyContinent implements Serializable
{
	private static final long serialVersionUID = 1L;

	@NotNull
	private String continentCode;
	@NotNull
	private String continentName;

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
		GeographyContinent that = (GeographyContinent) o;
		return Objects.equals(getContinentCode(), that.getContinentCode());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(getContinentCode());
	}

	@Override
	public String toString()
	{
		return "{" +
		       "continentCode='" + continentCode + '\'' +
		       ", continentName='" + continentName + '\'' +
		       '}';
	}
}
