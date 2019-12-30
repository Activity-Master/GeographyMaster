package com.guicedee.activitymaster.geography.services.dto.classifications;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class GeographyAsciiCode
		extends GeographyDefaultDto
		implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String code;
	private String name;
	private String nameAscii;
	private int geonameId;
}
