package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GeographyCity<J extends GeographyCity<J>>
		extends GeoNameDefaultData<J>
{

}
