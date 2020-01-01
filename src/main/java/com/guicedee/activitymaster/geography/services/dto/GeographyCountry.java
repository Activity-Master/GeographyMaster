package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Accessors(chain = true)
public class GeographyCountry
		implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final List<String> languages = new ArrayList<>();
	@Size(max = 2)
	private String iso;
	@Size(max = 3)
	private String iso3;
	@Size(max = 3)
	private String isoNumeric;
	@Size(max = 2)
	private String fps;
	private String countryName;
	private String postalCode;
	@Size(max = 3)
	private String webTld;
	private int areaSqlKM;
	private int population;
	private GeographyCoordinates coordinates;
	private int accuracy;
	private GeographyCurrency currency;
	private String countryDialCode;
	private String postalCodeDecimalFormat;
	private String postalCodeRegexFormat;
	private String capital;

	@Override
	public int hashCode()
	{
		return Objects.hash(getIso());
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
		GeographyCountry that = (GeographyCountry) o;
		return getIso().equals(that.getIso());
	}
}
