package com.guicedee.activitymaster.geography.services.dto.abstractions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of = "geonameId",callSuper = false)
public abstract class GeographyDefaultDto<J extends GeographyDefaultDto<J>>
		implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;
	private UUID geographyId;
	private Long geonameId;

	public J setGeographyId(UUID geographyId)
	{
		this.geographyId = geographyId;
		return (J) this;
	}

	public J setGeonameId(Long geoNameId)
	{
		this.geonameId = geoNameId;
		return (J) this;
	}
}
