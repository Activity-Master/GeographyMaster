package com.guicedee.activitymaster.geography.services.dto.classifications;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false,of = {"iso6392Code","iso6391Code"})
public class ISO639Language
		implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;

	private final Set<String> name = new HashSet<>();
	private final Set<String> frenchName = new HashSet<>();
	private final Set<String> germanName = new HashSet<>();

	private String iso6392Code;
	private String iso6391Code;
}
