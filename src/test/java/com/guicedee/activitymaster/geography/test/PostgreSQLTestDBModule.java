package com.guicedee.activitymaster.geography.test;

import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.DatabaseModule;
import com.guicedee.persistence.annotations.EntityManager;
import com.guicedee.persistence.implementations.postgres.PostgresConnectionBaseInfo;
import jakarta.validation.constraints.NotNull;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Testcontainers-backed PostgreSQL persistence unit for the geography integration tests.
 *
 * <p>Boots a throwaway PostgreSQL instance, applies the canonical FSDM schema scripts
 * ({@code postgres_fsdm.sql} + {@code postgres_structure.sql}, copied from the core module) and
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
        try {
            runScript("postgres_fsdm.sql", "/tmp/init_fsdm.sql");
            runScript("postgres_structure.sql", "/tmp/init_structure.sql");
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL initialization scripts", e);
        }
    }

    private static void runScript(String resourceName, String containerPath) throws Exception {
        Path scriptPath = Paths.get("src/test/resources/" + resourceName);
        postgresContainer.copyFileToContainer(MountableFile.forHostPath(scriptPath), containerPath);

        Container.ExecResult result = postgresContainer.execInContainer(
                "psql",
                "-v", "ON_ERROR_STOP=1",
                "-U", postgresContainer.getUsername(),
                "-d", postgresContainer.getDatabaseName(),
                "-f", containerPath
        );

        if (result.getExitCode() != 0) {
            System.err.println("[" + resourceName + " STDERR] " + result.getStderr());
            throw new RuntimeException("psql script '" + resourceName + "' failed: " + result.getStderr());
        }
        System.out.println("✅ Executed " + resourceName);
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

