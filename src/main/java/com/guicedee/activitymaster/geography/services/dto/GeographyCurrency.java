package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of = "currencyCode",callSuper = false)
public class GeographyCurrency
		implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;

	private String currencyCode;
	private String currencyName;
}
