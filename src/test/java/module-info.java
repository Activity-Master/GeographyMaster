import com.guicedee.client.services.lifecycle.IGuiceModule;

open module activity.master.geography.tests {
    requires transitive com.entityassist;
    requires transitive com.guicedee.persistence;

    requires org.junit.jupiter.api;

    requires jakarta.xml.bind;
    requires jakarta.persistence;
    requires jakarta.validation;

    requires transitive org.hibernate.reactive;
    requires io.smallrye.mutiny;
    requires com.google.guice;
    requires static lombok;

    requires org.testcontainers;
    requires io.vertx.sql.client.pg;
    requires jdk.httpserver;

    requires com.guicedee.activitymaster.fsdm;
    requires com.guicedee.activitymaster.fsdm.client;
    requires com.guicedee.activitymaster.geography;

    requires org.apache.commons.csv;

    requires com.graphqljava;

    requires com.guicedee.vertx.graphql;

    uses com.guicedee.vertx.graphql.services.IGraphQLSchemaProvider;

    provides IGuiceModule with com.guicedee.activitymaster.geography.test.PostgreSQLTestDBModule;
}


