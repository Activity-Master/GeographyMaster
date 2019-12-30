package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class GeoNameDefaultData<J extends GeoNameDefaultData<J>>
		extends GeographyDefaultDto
		implements Serializable
{
	private static final long serialVersionUID = 1L;
	private final List<String> alternateNames = new ArrayList<>();
	private String name;
	private String asciiname;
	private GeographyCoordinates coordinates;

	private char featureClass;
	@Size(min = 2,
			max = 4)
	private String featureCode;
	@Size(max = 2)
	private String countryCode;

	private String admin1Code;
	private String admin2Code;
	private String admin3Code;
	private String admin4Code;

	private int population;
	private int dem;
	@NotNull
	private GeographyTimezone timezone;

	private LocalDateTime modificationDate;
}
