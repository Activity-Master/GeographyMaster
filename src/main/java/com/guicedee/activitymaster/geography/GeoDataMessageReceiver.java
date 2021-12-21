package com.guicedee.activitymaster.geography;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.guicedee.activitymaster.fsdm.client.services.IInvolvedPartyService;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.party.IInvolvedParty;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.geography.implementations.GeographySystem;
import com.guicedee.activitymaster.sessions.services.IUserSession;
import com.guicedee.activitymaster.sessions.services.IUserSessionService;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedinjection.representations.IJsonRepresentation;
import com.guicedee.guicedservlets.websockets.options.WebSocketMessageReceiver;
import com.guicedee.guicedservlets.websockets.services.IWebSocketMessageReceiver;
import com.jwebmp.plugins.security.ipgeography.GeoData;

import java.util.Set;
import java.util.UUID;

import static com.guicedee.activitymaster.geography.services.IGeographyService.*;
import static com.guicedee.guicedinjection.GuiceContext.*;

public class GeoDataMessageReceiver
		implements IWebSocketMessageReceiver
{
	@Inject
	@Named(GeographySystemName)
	private ISystems<?,?> system;
	@Inject
	@Named(GeographySystemName)
	private UUID identityToken;
	@Override
	public Set<String> messageNames()
	{
		return Set.of("GeoData");
	}
	
	@Override
	public void receiveMessage(WebSocketMessageReceiver message) throws SecurityException
	{
		IUserSessionService<?> sessionMasterService = get(IUserSessionService.class);

		try
		{
			String js = get(ObjectMapper.class).writeValueAsString(message.getData());
			GeoData data = IJsonRepresentation.From(js, GeoData.class);
			if (data.getLocalStorage() != null)
			{
				if (data.getSuccess() != null && data.getSuccess())
				{
					IInvolvedPartyService<?> involvedPartyService = get(IInvolvedPartyService.class);
					IInvolvedParty<?, ?> involvedParty = involvedPartyService.get()
					                                                         .builder()
					                                                         .findByIdentificationType("IdentificationTypeWebClientUUID", data.getLocalStorage(), system, identityToken)
					                                                         .get()
					                                                         .orElse(null);
					if (involvedParty == null)
					{
						return;
					}
					
					if (data.getIp() != null)
					{
						//addressService.addOrFindIPAddress(data.getIp(),)
					}
				
					UUID token = GuiceContext.get(GeographySystem.class)
					                         .getSystemToken(involvedParty.getEnterprise());
					ISystems<?,?> system = GuiceContext.get(GeographySystem.class)
					                                   .getSystem(involvedParty.getEnterprise());
					IUserSession<?> sesion = sessionMasterService.getSession(involvedParty, system, token);
					sesion.setInvolvedParty(involvedParty);
					sesion.setSystem(system);
					sesion.addValue("geo-data", data);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
}
