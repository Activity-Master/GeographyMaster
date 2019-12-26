package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GeographyMunicipalArea<J extends GeographyMunicipalArea<J>>
		extends GeoNameDefaultData<J>
{

}
