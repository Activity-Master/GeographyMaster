import com.guicedee.activitymaster.fsdm.client.services.systems.IMasterSystem;
import com.guicedee.activitymaster.geography.implementations.*;

module com.guicedee.activitymaster.geography {

	exports com.guicedee.activitymaster.geography.services;
	exports com.guicedee.activitymaster.geography.services.dto;
	exports com.guicedee.activitymaster.geography.services.dto.classifications;

	requires com.guicedee.activitymaster.fsdm;
	requires com.guicedee.activitymaster.sessions;


	// OpenAPI 3.1 (Swagger) annotation support



	requires cache.api;

	requires org.apache.commons.csv;



	requires java.net.http;

	requires static lombok;
	uses com.guicedee.activitymaster.geography.services.IGeographyCountryResourceProvider;

	provides IMasterSystem with GeographySystem;
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
	opens com.guicedee.activitymaster.geography.rest to com.google.guice, com.guicedee.rest, tools.jackson.databind, org.hibernate.reactive, net.bytebuddy;

	exports geodata;
	opens geodata;
}
