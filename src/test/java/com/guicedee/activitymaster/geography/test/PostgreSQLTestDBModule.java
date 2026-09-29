package com.guicedee.activitymaster.geography.test;

import com.guicedee.activitymaster.fsdm.db.FsdmSchema;

import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.DatabaseModule;
import com.guicedee.persistence.annotations.EntityManager;
import com.guicedee.persistence.implementations.postgres.PostgresConnectionBaseInfo;
import jakarta.validation.constraints.NotNull;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;




import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Testcontainers-backed PostgreSQL persistence unit for the geography integration tests.
 *
 * <p>Boots a throwaway PostgreSQL instance, applies the canonical FSDM schema scripts
 * (the ordered {@code db/*.sql} scripts, via {@link FsdmSchema}) and
 * exposes it as the default {@code ActivityMaster-Test} entity manager — exactly the unit the rest
 * of the ActivityMaster reactive stack binds to.</p>
 */
@EntityManager(value = "ActivityMaster-Test", defaultEm = true)
public class PostgreSQLTestDBModule
        extends DatabaseModule<PostgreSQLTestDBModule>
        implements IGuiceModule<PostgreSQLTestDBModule>
{
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("fsdm")
            .withUsername("postgres")
            .withPassword("postgres");

    static {
        postgresContainer.start();
        System.setProperty("ENVIRONMENT", "test");
        System.setProperty("FSDM_SSL_MODE", "disable");
        System.setProperty("FSDM_PASSWORD", postgresContainer.getPassword());
        System.setProperty("FSDM_DBSERVER", "127.0.0.1");
        System.setProperty("FSDM_DBPORT", String.valueOf(postgresContainer.getFirstMappedPort()));
        System.setProperty("FSDM_DBNAME", postgresContainer.getDatabaseName());
        System.setProperty("FSDM_USER", postgresContainer.getUsername());
        try {
            FsdmSchema.forEachScript((script, sql) -> {
                postgresContainer.copyFileToContainer(Transferable.of(sql.getBytes(StandardCharsets.UTF_8)),
                        "/tmp/" + script);
                var scriptResult = postgresContainer.execInContainer("psql", "-v", "ON_ERROR_STOP=1",
                        "-U", postgresContainer.getUsername(), "-d", postgresContainer.getDatabaseName(), "-f", "/tmp/" + script);
                if (scriptResult.getExitCode() != 0) {
                    throw new IllegalStateException("psql failed on " + script + ": " + scriptResult.getStderr());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL initialization scripts", e);
        }
    }

    @NotNull
    @Override
    protected String getPersistenceUnitName()
    {
        return "ActivityMaster-Test";
    }

    @Override
    @NotNull
    protected ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties)
    {
        PostgresConnectionBaseInfo connectionInfo = new PostgresConnectionBaseInfo();
        connectionInfo.setServerName(postgresContainer.getHost());
        connectionInfo.setPort(String.valueOf(postgresContainer.getFirstMappedPort()));
        connectionInfo.setDatabaseName(postgresContainer.getDatabaseName());
        connectionInfo.setUsername(postgresContainer.getUsername());
        connectionInfo.setPassword(postgresContainer.getPassword());
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(true);
        return connectionInfo;
    }

    @NotNull
    @Override
    protected String getJndiMapping()
    {
        return "jdbc:activitymaster-test";
    }

    @Override
    public Integer sortOrder()
    {
        return 10;
    }
}

