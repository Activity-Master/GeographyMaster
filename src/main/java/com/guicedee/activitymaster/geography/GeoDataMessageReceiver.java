package com.guicedee.activitymaster.geography;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guicedee.activitymaster.core.services.dto.IInvolvedParty;
import com.guicedee.activitymaster.core.services.dto.ISystems;
import com.guicedee.activitymaster.core.services.system.IInvolvedPartyService;
import com.guicedee.activitymaster.geography.implementations.GeographySystem;
import com.guicedee.activitymaster.sessions.services.ISession;
import com.guicedee.activitymaster.sessions.services.ISessionMasterService;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedservlets.websockets.options.WebSocketMessageReceiver;
import com.guicedee.guicedservlets.websockets.services.IWebSocketMessageReceiver;
import com.jwebmp.plugins.security.ipgeography.GeoData;

import java.util.Set;
import java.util.UUID;

import static com.guicedee.guicedinjection.GuiceContext.*;

public class GeoDataMessageReceiver
		implements IWebSocketMessageReceiver
{
	
	@Override
	public Set<String> messageNames()
	{
		return Set.of("GeoData");
	}
	
	@Override
	public void receiveMessage(WebSocketMessageReceiver message) throws SecurityException
	{
		try
		{
			String js = get(ObjectMapper.class).writeValueAsString(message.getData());
			GeoData data = new GeoData().From(js, GeoData.class);
			if (data.getLocalStorage() != null)
			{
				if (data.getSuccess() != null && data.getSuccess())
				{
					IInvolvedPartyService<?> involvedPartyService = get(IInvolvedPartyService.class);
					IInvolvedParty<?> involvedParty = involvedPartyService.findByIdentificationType("IdentificationTypeWebClientUUID", data.getLocalStorage());
					if (involvedParty == null)
					{
						return;
					}
					
					if (data.getIp() != null)
					{
						//addressService.addOrFindIPAddress(data.getIp(),)
					}
					ISessionMasterService<?> sessionMasterService = get(ISessionMasterService.class);
					ISystems<?> system = get(GeographySystem.class)
							.getSystem(involvedParty.getEnterprise());
					UUID token = GuiceContext.get(GeographySystem.class)
					                         .getSystemToken(involvedParty.getEnterprise());
					ISession<?> sesion = sessionMasterService.getSession(involvedParty, system, token);
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
