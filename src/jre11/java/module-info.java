import com.guicedee.activitymaster.geography.GeoDataMessageReceiver;
import com.guicedee.activitymaster.geography.implementations.*;
import com.guicedee.guicedservlets.websockets.services.IWebSocketMessageReceiver;

module com.guicedee.activitymaster.geography {

	exports com.guicedee.activitymaster.geography.services;
	exports com.guicedee.activitymaster.geography.services.dto;
	exports com.guicedee.activitymaster.geography.services.dto.classifications;

	requires com.guicedee.activitymaster.core;
	requires com.guicedee.guicedinjection;
	requires com.google.guice;
	requires com.guicedee.activitymaster.sessions;

	requires com.fasterxml.jackson.databind;

	requires jakarta.validation;

	requires cache.api;

	requires static lombok;
	requires org.apache.commons.csv;
	requires org.apache.commons.text;
	requires org.apache.commons.lang3;

	requires com.guicedee.guicedservlets.websockets;
	requires com.jwebmp.plugins.security.ipgeography;
	requires com.entityassist;

	requires com.guicedee.guicedpersistence;

	provides com.guicedee.activitymaster.core.services.IActivityMasterSystem with GeographySystem;
	provides IWebSocketMessageReceiver with GeoDataMessageReceiver;
	provides com.guicedee.guicedinjection.interfaces.IGuiceModule with GeographyBinder;
	provides com.guicedee.guicedinjection.interfaces.IGuiceScanModuleInclusions with GeographerMasterModuleInclusion;
	
	opens com.guicedee.activitymaster.geography to com.google.guice;
	opens com.guicedee.activitymaster.geography.implementations.updates to com.google.guice;
	opens com.guicedee.activitymaster.geography.implementations to com.google.guice;

	opens geodata;
}
