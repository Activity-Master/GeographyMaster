package com.guicedee.activitymaster.geography.services.exceptions;

import com.guicedee.activitymaster.client.services.exceptions.ActivityMasterException;

public class GeographyException
		extends ActivityMasterException
{
	public GeographyException()
	{
	}

	public GeographyException(String message)
	{
		super(message);
	}

	public GeographyException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public GeographyException(Throwable cause)
	{
		super(cause);
	}

	public GeographyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
