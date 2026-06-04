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

	requires com.guicedee.rest;
	requires com.guicedee.vertx;
	requires com.guicedee.vertx.graphql;
	requires com.graphqljava;
	requires io.vertx.core;
	requires jakarta.ws.rs;

	requires com.fasterxml.jackson.databind;

	requires jakarta.validation;

	requires cache.api;

	requires org.apache.commons.csv;
	requires org.apache.commons.lang3;

	requires com.entityassist;

	requires com.guicedee.activitymaster.fsdm.client;

	requires java.net.http;

	requires io.smallrye.mutiny;
	requires org.hibernate.reactive;
	requires static lombok;
	requires org.apache.logging.log4j;

	provides IActivityMasterSystem with GeographySystem;
	provides com.guicedee.client.services.lifecycle.IGuiceModule with GeographyBinder;
	provides com.guicedee.client.services.config.IGuiceScanModuleInclusions with GeographerMasterModuleInclusion;
	provides com.guicedee.vertx.graphql.services.IGraphQLSchemaProvider
			with com.guicedee.activitymaster.geography.implementations.graphql.GeographyGraphQLSchemaProvider;

	exports com.guicedee.activitymaster.geography;
	exports com.guicedee.activitymaster.geography.events;
	exports com.guicedee.activitymaster.geography.implementations.updates;
	exports com.guicedee.activitymaster.geography.rest;

	opens com.guicedee.activitymaster.geography to com.google.guice;
	opens com.guicedee.activitymaster.geography.events to com.google.guice, com.guicedee.vertx;
	opens com.guicedee.activitymaster.geography.implementations.updates to com.google.guice;
	opens com.guicedee.activitymaster.geography.implementations to com.google.guice;
	opens com.guicedee.activitymaster.geography.implementations.graphql to com.google.guice;
	opens com.guicedee.activitymaster.geography.rest to com.google.guice, com.guicedee.rest, com.fasterxml.jackson.databind, org.hibernate.reactive, net.bytebuddy;

	exports geodata;
	opens geodata;
}
