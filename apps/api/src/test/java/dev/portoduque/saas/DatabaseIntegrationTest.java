package dev.portoduque.saas;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@Testcontainers
class DatabaseIntegrationTest {

    private static final String DATABASE_NAME = "saas_skeleton";
    private static final String DATABASE_USER = "saas_skeleton";
    private static final String DATABASE_PASSWORD = "integration-test-only";
    private static final Network NETWORK = Network.newNetwork();

    @Container
    static final GenericContainer<?> POSTGRES = new GenericContainer<>(
                    DockerImageName.parse("postgres:18.1-alpine"))
            .withEnv("POSTGRES_DB", DATABASE_NAME)
            .withEnv("POSTGRES_USER", DATABASE_USER)
            .withEnv("POSTGRES_PASSWORD", DATABASE_PASSWORD)
            .withExposedPorts(5432)
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres")
            .withCommand(
                    "postgres",
                    "-c",
                    "shared_preload_libraries=pg_stat_statements",
                    "-c",
                    "compute_query_id=on");

    @Container
    static final GenericContainer<?> PGBOUNCER = new GenericContainer<>(
                    DockerImageName.parse("pgbouncer/pgbouncer:1.24.1"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "infra", "pgbouncer", "entrypoint.sh")),
                    "/tmp/saas-pgbouncer-entrypoint.sh")
            .withCreateContainerCmdModifier(
                    command -> command.withEntrypoint("/tmp/saas-pgbouncer-entrypoint.sh"))
            .withExposedPorts(6432)
            .withNetwork(NETWORK)
            .withEnv("DATABASES_HOST", "postgres")
            .withEnv("DATABASES_PORT", "5432")
            .withEnv("DATABASES_DBNAME", DATABASE_NAME)
            .withEnv("DATABASES_USER", DATABASE_USER)
            .withEnv("DATABASES_PASSWORD", DATABASE_PASSWORD)
            .withEnv("PGBOUNCER_AUTH_TYPE", "scram-sha-256")
            .withEnv("PGBOUNCER_POOL_MODE", "session")
            .withEnv("PGBOUNCER_LISTEN_PORT", "6432")
            .waitingFor(Wait.forListeningPort())
            .dependsOn(POSTGRES);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/%s"
                .formatted(PGBOUNCER.getHost(), PGBOUNCER.getMappedPort(6432), DATABASE_NAME));
        registry.add("spring.datasource.username", () -> DATABASE_USER);
        registry.add("spring.datasource.password", () -> DATABASE_PASSWORD);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void migrates_clean_postgresql_18_database_through_pgbouncer() throws SQLException {
        Integer serverVersion = jdbcTemplate.queryForObject(
                "select current_setting('server_version_num')::integer", Integer.class);
        List<String> installedExtensions = jdbcTemplate.queryForList(
                "select extname from pg_extension where extname = 'pg_stat_statements'", String.class);
        List<String> successfulMigrations = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        List<String> applicationTables = jdbcTemplate.queryForList(
                """
                select table_name
                  from information_schema.tables
                 where table_schema = 'public'
                   and table_name in ('users', 'auth_tokens', 'auth_rate_limits', 'spring_session',
                                      'spring_session_attributes', 'organizations', 'organization_memberships')
                 order by table_name
                """,
                String.class);

        assertThat(serverVersion).isGreaterThanOrEqualTo(180000);
        assertThat(installedExtensions).containsExactly("pg_stat_statements");
        assertThat(successfulMigrations).containsExactly("1", "2", "3");
        assertThat(applicationTables)
                .containsExactly(
                        "auth_rate_limits",
                        "auth_tokens",
                        "organization_memberships",
                        "organizations",
                        "spring_session",
                        "spring_session_attributes",
                        "users");
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL())
                    .contains(":" + PGBOUNCER.getMappedPort(6432) + "/")
                    .doesNotContain(":" + POSTGRES.getMappedPort(5432) + "/");
        }
    }
}
