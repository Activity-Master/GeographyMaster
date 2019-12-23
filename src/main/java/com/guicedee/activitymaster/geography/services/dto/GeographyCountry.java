package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.core.services.dto.IAddress;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;


@Data
public class GeographyCountry
{
	@Size(max=2)
	private String iso;
	@Size(max=3)
	private String iso3;
	@Size(max=3)
	private String isoNumeric;
	@Size(max=2)
	@Null
	private String fps;
	@NotNull
	private String countryName;
	@Size(max=3)
	@Null
	private String webTld;

	private int areaSqlKM;
	private int population;

	private String currencyCode;
	private String currencyName;

	private IAddress<?> countryDialCode;

	private String postalCodeDecimalFormat;
	private String postalCodeRegexFormat;

	private String capital;
	private final List<String> languages = new ArrayList<>();


}
