module com.guicedee.activitymaster.geography {

	exports com.guicedee.activitymaster.geography;

	requires com.guicedee.activitymaster.core;
	requires com.guicedee.guicedinjection;
	requires com.google.guice;

	provides com.guicedee.activitymaster.core.services.IActivityMasterSystem with com.guicedee.activitymaster.geography.GeographyMasterSystem;

	opens com.guicedee.activitymaster.geography to com.google.guice;
}
