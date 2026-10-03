package dev.portoduque.saas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.portoduque.saas.shared.api.ApiProblemException;
import dev.portoduque.saas.users.UserAccount;
import dev.portoduque.saas.users.UserAccountRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthenticationIntegrationTest {

    private static final String DATABASE_NAME = "saas_skeleton";
    private static final String DATABASE_USER = "saas_skeleton";
    private static final String DATABASE_PASSWORD = "integration-test-only";
    private static final String VALID_PASSWORD = "correct horse battery staple";
    private static final Network NETWORK = Network.newNetwork();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Container
    static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:18.1-alpine"))
            .withEnv("POSTGRES_DB", DATABASE_NAME)
            .withEnv("POSTGRES_USER", DATABASE_USER)
            .withEnv("POSTGRES_PASSWORD", DATABASE_PASSWORD)
            .withExposedPorts(5432)
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres")
            .withCommand("postgres", "-c", "shared_preload_libraries=pg_stat_statements", "-c", "compute_query_id=on");

    @Container
    static final GenericContainer<?> PGBOUNCER = new GenericContainer<>(DockerImageName.parse("pgbouncer/pgbouncer:1.24.1"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "infra", "pgbouncer", "entrypoint.sh")),
                    "/tmp/saas-pgbouncer-entrypoint.sh")
            .withCreateContainerCmdModifier(command -> command.withEntrypoint("/tmp/saas-pgbouncer-entrypoint.sh"))
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
        registry.add("server.servlet.session.cookie.secure", () -> "true");
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registers_a_normalized_user_with_uuidv7_and_argon2id_without_exposing_the_password() throws Exception {
        SessionClient client = new SessionClient();
        client.refreshCsrf();

        HttpResponse<String> response = client.post(
                "/api/v1/auth/registrations",
                "{\"email\":\"Person.One@Example.COM\",\"password\":\"" + VALID_PASSWORD + "\"}");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("email").asText()).isEqualTo("person.one@example.com");
        assertThat(body.path("emailVerified").asBoolean()).isFalse();
        assertThat(java.util.UUID.fromString(body.path("id").asText()).version()).isEqualTo(7);
        assertThat(response.body()).doesNotContain(VALID_PASSWORD).doesNotContain("passwordHash");

        UserAccount persisted = users.findByEmail("person.one@example.com").orElseThrow();
        assertThat(persisted.getPasswordHash()).startsWith("$argon2id$");
        assertThat(passwordEncoder.matches(VALID_PASSWORD, persisted.getPasswordHash())).isTrue();
    }

    @Test
    void rejects_missing_csrf_invalid_credentials_and_duplicate_identity_with_safe_problems() throws Exception {
        SessionClient client = new SessionClient();
        HttpResponse<String> missingCsrf = client.postWithoutCsrf(
                "/api/v1/auth/registrations",
                "{\"email\":\"csrf@example.com\",\"password\":\"" + VALID_PASSWORD + "\"}");
        assertProblem(missingCsrf, 403, "ACCESS_DENIED");

        client.refreshCsrf();
        assertThat(client.post(
                                "/api/v1/auth/registrations",
                                "{\"email\":\"duplicate@example.com\",\"password\":\"" + VALID_PASSWORD + "\"}")
                        .statusCode())
                .isEqualTo(201);
        assertProblem(
                client.post(
                        "/api/v1/auth/registrations",
                        "{\"email\":\"DUPLICATE@example.com\",\"password\":\"" + VALID_PASSWORD + "\"}"),
                409,
                "EMAIL_ALREADY_REGISTERED");
        assertProblem(
                client.post(
                        "/api/v1/auth/sessions",
                        "{\"email\":\"duplicate@example.com\",\"password\":\"wrong-password-value\"}"),
                401,
                "INVALID_CREDENTIALS");
    }

    @Test
    void rotates_and_persists_the_session_then_invalidates_it_on_logout() throws Exception {
        String email = "session@example.com";
        SessionClient client = registeredClient(email);
        String anonymousSession = client.sessionId;

        HttpResponse<String> login = client.post(
                "/api/v1/auth/sessions",
                "{\"email\":\"" + email + "\",\"password\":\"" + VALID_PASSWORD + "\"}");

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(client.sessionId).isNotBlank().isNotEqualTo(anonymousSession);
        assertThat(login.headers().firstValue("Set-Cookie").orElseThrow())
                .contains("HttpOnly")
                .contains("Secure")
                .containsIgnoringCase("SameSite=Lax");
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from spring_session where principal_name = ?", Integer.class, email))
                .isEqualTo(1);

        assertProblem(client.delete("/api/v1/auth/session"), 403, "ACCESS_DENIED");
        client.refreshCsrf();
        assertThat(client.get("/api/v1/auth/session").statusCode()).isEqualTo(200);
        assertThat(client.delete("/api/v1/auth/session").statusCode()).isEqualTo(204);
        assertProblem(client.get("/api/v1/auth/session"), 401, "UNAUTHENTICATED");
    }

    @Test
    void rate_limits_repeated_login_failures_without_storing_the_raw_identity() throws Exception {
        String email = "rate-limit@example.com";
        SessionClient client = registeredClient(email);
        String payload = "{\"email\":\"" + email + "\",\"password\":\"wrong-password-value\"}";

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertProblem(client.post("/api/v1/auth/sessions", payload), 401, "INVALID_CREDENTIALS");
        }
        assertProblem(client.post("/api/v1/auth/sessions", payload), 429, "RATE_LIMITED");
        assertThat(jdbcTemplate.queryForObject(
                        "select subject_hash from auth_rate_limits where action = 'LOGIN' and subject_hash = ?",
                        String.class,
                        SecretHashing.sha256(email)))
                .hasSize(64)
                .doesNotContain(email);
    }

    @Test
    void verification_and_reset_tokens_are_hashed_expiring_and_single_use() {
        UserAccount user = authService.register("tokens@example.com", VALID_PASSWORD);

        IssuedAuthToken verification = authService.issueEmailVerification(user.getId());
        assertThat(jdbcTemplate.queryForObject(
                        "select token_hash from auth_tokens where token_hash = ?",
                        String.class,
                        SecretHashing.sha256(verification.value())))
                .isEqualTo(SecretHashing.sha256(verification.value()))
                .isNotEqualTo(verification.value());
        authService.verifyEmail(verification.value());
        assertThat(users.findById(user.getId()).orElseThrow().isEmailVerified()).isTrue();
        assertThatThrownBy(() -> authService.verifyEmail(verification.value()))
                .isInstanceOf(ApiProblemException.class)
                .extracting(exception -> ((ApiProblemException) exception).code())
                .isEqualTo("INVALID_OR_EXPIRED_TOKEN");

        IssuedAuthToken reset = authService.issuePasswordReset(user.getEmail());
        String replacement = "a different secure password";
        authService.resetPassword(reset.value(), replacement);
        assertThat(passwordEncoder.matches(replacement, users.findById(user.getId()).orElseThrow().getPasswordHash()))
                .isTrue();
        assertThatThrownBy(() -> authService.resetPassword(reset.value(), VALID_PASSWORD))
                .isInstanceOf(ApiProblemException.class);

        IssuedAuthToken expired = authService.issuePasswordReset(user.getEmail());
        jdbcTemplate.update(
                "update auth_tokens set expires_at = now() - interval '1 second' where token_hash = ?",
                SecretHashing.sha256(expired.value()));
        assertThatThrownBy(() -> authService.resetPassword(expired.value(), VALID_PASSWORD))
                .isInstanceOf(ApiProblemException.class);
    }

    private SessionClient registeredClient(String email) throws Exception {
        SessionClient client = new SessionClient();
        client.refreshCsrf();
        HttpResponse<String> response = client.post(
                "/api/v1/auth/registrations",
                "{\"email\":\"" + email + "\",\"password\":\"" + VALID_PASSWORD + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return client;
    }

    private void assertProblem(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");
        JsonNode problem = objectMapper.readTree(response.body());
        assertThat(problem.path("code").asText()).isEqualTo(code);
        assertThat(problem.path("requestId").asText()).isNotBlank();
        assertThat(response.body().toLowerCase(Locale.ROOT))
                .doesNotContain("password_hash")
                .doesNotContain(VALID_PASSWORD);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private final class SessionClient {

        private String sessionId;
        private String csrfToken;
        private String csrfHeader;

        void refreshCsrf() throws Exception {
            HttpResponse<String> response = send("GET", "/api/v1/auth/csrf", null, false);
            assertThat(response.statusCode()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(response.body());
            csrfHeader = body.path("headerName").asText();
            csrfToken = body.path("token").asText();
        }

        HttpResponse<String> get(String path) throws Exception {
            return send("GET", path, null, false);
        }

        HttpResponse<String> post(String path, String body) throws Exception {
            return send("POST", path, body, true);
        }

        HttpResponse<String> postWithoutCsrf(String path, String body) throws Exception {
            return send("POST", path, body, false);
        }

        HttpResponse<String> delete(String path) throws Exception {
            return send("DELETE", path, null, true);
        }

        private HttpResponse<String> send(String method, String path, String body, boolean includeCsrf) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path));
            if (sessionId != null) {
                builder.header("Cookie", "SAAS_SESSION=" + sessionId);
            }
            if (includeCsrf && csrfToken != null) {
                builder.header(csrfHeader, csrfToken);
            }
            if (body != null) {
                builder.header("Content-Type", "application/json");
            }
            builder.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            response.headers().allValues("Set-Cookie").stream()
                    .filter(cookie -> cookie.startsWith("SAAS_SESSION="))
                    .findFirst()
                    .ifPresent(cookie -> sessionId = cookie.substring("SAAS_SESSION=".length(), cookie.indexOf(';')));
            return response;
        }
    }
}
