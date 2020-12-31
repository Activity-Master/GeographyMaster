package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import com.guicedee.activitymaster.geography.services.dto.classifications.ISO639Language;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of = "iso",callSuper = false)
public class GeographyCountry
		extends GeographyDefaultDto
		implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;

	private final List<ISO639Language> languages = new ArrayList<>();
	@Size(max = 2)
	private String iso;
	@Size(max = 3)
	private String iso3;
	@Size(max = 3)
	private String isoNumeric;
	private String fips;
	private String equivalentFips;
	private String countryName;
	private String postalCode;
	@Size(max = 3)
	private String webTld;
	private String areaSqlKM;
	private int population;
	private GeographyCoordinates coordinates;
	private int accuracy;

	private GeographyCurrency currency;

	private String countryDialCode;
	private String postalCodeDecimalFormat;
	private String postalCodeRegexFormat;
	private String capital;

	private GeographyContinent continent;
}
