package com.guicedee.activitymaster.geography.services.dto.abstractions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;


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

	public @org.jspecify.annotations.NonNull J setGeographyId(java.lang.String geographyId)
	{
		this.geographyId = geographyId;
		return (J) this;
	}

	public @org.jspecify.annotations.NonNull J setGeonameId(Long geoNameId)
	{
		this.geonameId = geoNameId;
		return (J) this;
	}
}
