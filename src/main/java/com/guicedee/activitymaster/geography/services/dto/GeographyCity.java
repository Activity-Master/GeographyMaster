package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class GeographyCity<J extends GeographyCity<J>>
		extends GeoNameDefaultData<J>
{

}
