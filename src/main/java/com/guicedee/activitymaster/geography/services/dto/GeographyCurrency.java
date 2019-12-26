package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class GeographyCurrency implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String currencyCode;
	private String currencyName;
}
