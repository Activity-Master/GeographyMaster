import com.guicedee.activitymaster.fsdm.client.services.systems.IActivityMasterSystem;
import com.guicedee.activitymaster.geography.implementations.*;

module com.guicedee.activitymaster.geography {

	exports com.guicedee.activitymaster.geography.services;
	exports com.guicedee.activitymaster.geography.services.dto;
	exports com.guicedee.activitymaster.geography.services.dto.classifications;

	requires com.guicedee.activitymaster.fsdm;
	requires com.guicedee.guicedinjection;
	requires com.guicedee.jsonrepresentation;
	requires com.google.guice;
	requires com.guicedee.activitymaster.sessions;

	requires com.fasterxml.jackson.databind;

	requires jakarta.validation;

	requires cache.api;

	requires org.apache.commons.csv;
	requires org.apache.commons.lang3;

	requires com.entityassist;

	requires com.guicedee.activitymaster.fsdm.client;

	requires io.smallrye.mutiny;
	requires org.hibernate.reactive;
	requires static lombok;
	requires org.apache.logging.log4j;

	provides IActivityMasterSystem with GeographySystem;
	provides com.guicedee.client.services.lifecycle.IGuiceModule with GeographyBinder;
	provides com.guicedee.client.services.config.IGuiceScanModuleInclusions with GeographerMasterModuleInclusion;

	exports com.guicedee.activitymaster.geography;
	exports com.guicedee.activitymaster.geography.implementations.updates;

	opens com.guicedee.activitymaster.geography to com.google.guice;
	opens com.guicedee.activitymaster.geography.implementations.updates to com.google.guice;
	opens com.guicedee.activitymaster.geography.implementations to com.google.guice;

	opens geodata;
}
