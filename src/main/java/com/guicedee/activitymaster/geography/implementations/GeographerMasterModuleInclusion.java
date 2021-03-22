package com.guicedee.activitymaster.geography.implementations;

import com.guicedee.guicedinjection.interfaces.IGuiceScanModuleInclusions;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public class GeographerMasterModuleInclusion implements IGuiceScanModuleInclusions<GeographerMasterModuleInclusion>
{
	@Override
	public @NotNull Set<String> includeModules()
	{
		Set<String> set = new HashSet<>();
		//set.add("com.guicedee.activitymaster.geography");
		return set;
	}
}
