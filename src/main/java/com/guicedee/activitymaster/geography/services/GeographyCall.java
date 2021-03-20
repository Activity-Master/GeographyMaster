package com.guicedee.activitymaster.geography.services;

import com.guicedee.activitymaster.client.services.builders.warehouse.party.IInvolvedParty;
import com.guicedee.activitymaster.geography.services.dto.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GeographyCall
{
	private IInvolvedParty<?,?> involvedParty;

	private String ipAddress;
	private String hostname;

	private IInvolvedParty<?,?> isp;
	private IInvolvedParty<?,?> ispOrganization;
	private IInvolvedParty<?,?> asn;
	private IInvolvedParty<?,?> asnOrg;

	private GeographyCoordinates coordinates;

	private String postalCode;

	private GeographyCity city;

	private GeographyCountry country;

	private GeographyMunicipalArea region;

	private GeographyContinent continent;

	private GeographyTimezone timezone;

	private GeographyCurrency currency;

}
