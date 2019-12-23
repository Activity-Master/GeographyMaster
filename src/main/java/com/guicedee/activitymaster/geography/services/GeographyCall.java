package com.guicedee.activitymaster.geography.services;

import com.guicedee.activitymaster.core.services.dto.IInvolvedParty;

public class GeographyCall
{
	private IInvolvedParty<?> involvedParty;

	private String ipAddress;
	private String hostname;

	private IInvolvedParty<?> isp;
	private IInvolvedParty<?> ispOrganization;
	private IInvolvedParty<?> asn;
	private IInvolvedParty<?> asnOrg;

	private String latitude;
	private String longitude;

	private String postalCode;

	private GeographyCity city;

	private GeographyCountry country;

	private GeographyRegion region;

	private GeographyContinent continent;

	private GeographyTimeZone timezone;

	private GeographyCurrency currency;


}
