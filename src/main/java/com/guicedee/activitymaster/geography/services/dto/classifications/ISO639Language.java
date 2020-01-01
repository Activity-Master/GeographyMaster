package com.guicedee.activitymaster.geography.services.dto.classifications;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
public class ISO639Language
		implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final Set<String> name = new HashSet<>();
	private final Set<String> frenchName = new HashSet<>();
	private final Set<String> germanName = new HashSet<>();

	private String iso6392Code;
	private String iso6391Code;

	@Override
	public int hashCode()
	{
		return Objects.hash(getIso6392Code(), getIso6391Code());
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
		ISO639Language language = (ISO639Language) o;
		return Objects.equals(getIso6392Code(), language.getIso6392Code()) &&
		       Objects.equals(getIso6391Code(), language.getIso6391Code());
	}
}
