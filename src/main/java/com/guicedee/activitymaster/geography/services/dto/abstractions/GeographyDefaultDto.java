package com.guicedee.activitymaster.geography.services.dto.abstractions;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public abstract class GeographyDefaultDto
		implements Serializable
{
	private static final long serialVersionUID = 1L;
	private Long geographyId;
	private Long geonameId;
}
