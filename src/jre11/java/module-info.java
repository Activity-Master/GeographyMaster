module com.armineasy.activitymaster.geography {

	exports com.armineasy.activitymaster.geography;

	requires com.armineasy.activitymaster.activitymaster;
	requires com.guicedee.guicedinjection;
	requires com.google.guice;

	provides com.armineasy.activitymaster.activitymaster.services.IActivityMasterSystem with com.armineasy.activitymaster.geography.GeographyMasterSystem;

	opens com.armineasy.activitymaster.geography to com.google.guice;
}
