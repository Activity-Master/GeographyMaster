package com.guicedee.activitymaster.geography.services.dto.classifications;

import com.guicedee.activitymaster.geography.services.dto.abstractions.GeographyDefaultDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false,of = "name")
public class GeographyAsciiCode
		extends GeographyDefaultDto
		implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;

	private String code;
	private String name;
	private String nameAscii;
}
