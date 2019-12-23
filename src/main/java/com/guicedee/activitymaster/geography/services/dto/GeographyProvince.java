package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class GeographyProvince
{
	private String name;
	private String asciiname;
	private final List<String> alternateNames = new ArrayList<>();
	private String latitude;
	private String longitude;
	private char featureClass;
	@Size(min=2,max=4)
	private String featureCode;
	@Size(max=2)
	private String countryCode;

	private String admin1Code;
	private String admin2Code;
	private String admin3Code;
	private String admin4Code;

	private int population;
	private int dem;
	@NotNull
	private Timezone timezone;

	private LocalDateTime modificationDate;
}
