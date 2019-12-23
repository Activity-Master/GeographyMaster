import com.guicedee.activitymaster.geography.GeoDataMessageReceiver;
import com.guicedee.activitymaster.geography.implementations.GeographyBinder;
import com.guicedee.guicedservlets.websockets.services.IWebSocketMessageReceiver;

module com.guicedee.activitymaster.geography {

	exports com.guicedee.activitymaster.geography.services;

	requires com.guicedee.activitymaster.core;
	requires com.guicedee.guicedinjection;
	requires com.google.guice;
	requires com.guicedee.activitymaster.sessions;

	requires com.fasterxml.jackson.databind;

	requires java.validation;

	requires static lombok;

	requires com.guicedee.guicedservlets.websockets;
	requires com.jwebmp.plugins.security.ipgeography;

	provides com.guicedee.activitymaster.core.services.IActivityMasterSystem with com.guicedee.activitymaster.geography.GeographyMasterSystem;
	provides IWebSocketMessageReceiver with GeoDataMessageReceiver;
	provides com.guicedee.guicedinjection.interfaces.IGuiceModule with GeographyBinder;

	opens com.guicedee.activitymaster.geography to com.google.guice;
}
