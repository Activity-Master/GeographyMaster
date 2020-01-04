package com.guicedee.activitymaster.geography.services.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Objects;

@Data
@Accessors(chain = true)
public class GeographyCurrency
		implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String currencyCode;
	private String currencyName;

	@Override
	public int hashCode()
	{
		return Objects.hash(getCurrencyCode());
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		GeographyCurrency that = (GeographyCurrency) o;
		return Objects.equals(getCurrencyCode(), that.getCurrencyCode());
	}
}
