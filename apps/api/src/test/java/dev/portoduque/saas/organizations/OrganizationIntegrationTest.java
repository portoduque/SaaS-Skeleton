package dev.portoduque.saas.organizations;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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
class OrganizationIntegrationTest {

    private static final String DATABASE_NAME = "saas_skeleton";
    private static final String DATABASE_USER = "saas_skeleton";
    private static final String DATABASE_PASSWORD = "integration-test-only";
    private static final String PASSWORD = "correct horse battery staple";
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

    @Test
    void creates_lists_and_selects_multiple_organizations_for_the_authenticated_user() throws Exception {
        SessionClient owner = registeredClient("multi-owner@example.com");

        JsonNode first = body(owner.post("/api/v1/organizations", "{\"name\":\"First Workspace\"}"), 201);
        JsonNode second = body(owner.post("/api/v1/organizations", "{\"name\":\"Second Workspace\"}"), 201);

        assertThat(first.path("role").asText()).isEqualTo("OWNER");
        assertThat(second.path("role").asText()).isEqualTo("OWNER");
        assertThat(java.util.UUID.fromString(first.path("id").asText()).version()).isEqualTo(7);

        JsonNode page = body(owner.get("/api/v1/organizations?page=0&size=10"), 200);
        assertThat(page.path("content")).hasSize(2);
        assertThat(page.path("totalElements").asInt()).isEqualTo(2);

        JsonNode selected = body(owner.put(
                "/api/v1/organization-selection",
                "{\"organizationId\":\"" + second.path("id").asText() + "\"}"), 200);
        assertThat(selected.path("organizationId").asText()).isEqualTo(second.path("id").asText());
        assertThat(selected.path("role").asText()).isEqualTo("OWNER");
        assertThat(body(owner.get("/api/v1/organization-selection"), 200)
                        .path("organizationId")
                        .asText())
                .isEqualTo(second.path("id").asText());
    }

    @Test
    void enforces_owner_only_membership_management_and_role_changes() throws Exception {
        SessionClient owner = registeredClient("roles-owner@example.com");
        SessionClient admin = registeredClient("roles-admin@example.com");
        SessionClient member = registeredClient("roles-member@example.com");
        String organizationId = body(owner.post(
                        "/api/v1/organizations", "{\"name\":\"Roles Workspace\"}"), 201)
                .path("id")
                .asText();

        JsonNode adminMembership = body(owner.post(
                "/api/v1/organizations/" + organizationId + "/memberships",
                "{\"email\":\"roles-admin@example.com\",\"role\":\"ADMIN\"}"), 201);
        JsonNode memberMembership = body(owner.post(
                "/api/v1/organizations/" + organizationId + "/memberships",
                "{\"email\":\"roles-member@example.com\",\"role\":\"MEMBER\"}"), 201);

        assertProblem(admin.post(
                "/api/v1/organizations/" + organizationId + "/memberships",
                "{\"email\":\"another@example.com\",\"role\":\"MEMBER\"}"), 403, "OWNER_ROLE_REQUIRED");
        assertProblem(member.patch(
                "/api/v1/organizations/" + organizationId + "/memberships/"
                        + adminMembership.path("id").asText(),
                "{\"role\":\"MEMBER\"}"), 403, "OWNER_ROLE_REQUIRED");

        JsonNode promoted = body(owner.patch(
                "/api/v1/organizations/" + organizationId + "/memberships/"
                        + memberMembership.path("id").asText(),
                "{\"role\":\"ADMIN\"}"), 200);
        assertThat(promoted.path("role").asText()).isEqualTo("ADMIN");

        assertThat(owner.delete("/api/v1/organizations/" + organizationId + "/memberships/"
                                + adminMembership.path("id").asText())
                        .statusCode())
                .isEqualTo(204);
        assertProblem(admin.put(
                "/api/v1/organization-selection",
                "{\"organizationId\":\"" + organizationId + "\"}"), 404, "ORGANIZATION_NOT_FOUND");
    }

    @Test
    void prevents_cross_organization_access_duplicate_membership_and_owner_mutation() throws Exception {
        SessionClient owner = registeredClient("guard-owner@example.com");
        SessionClient outsider = registeredClient("guard-outsider@example.com");
        String organizationId = body(owner.post(
                        "/api/v1/organizations", "{\"name\":\"Guarded Workspace\"}"), 201)
                .path("id")
                .asText();
        JsonNode ownerMembership = body(owner.get(
                "/api/v1/organizations/" + organizationId + "/memberships?page=0&size=10"), 200)
                .path("content")
                .get(0);

        assertProblem(outsider.get("/api/v1/organizations/" + organizationId), 404, "ORGANIZATION_NOT_FOUND");
        assertProblem(outsider.get(
                "/api/v1/organizations/" + organizationId + "/memberships?page=0&size=10"), 404,
                "ORGANIZATION_NOT_FOUND");
        assertProblem(owner.post(
                "/api/v1/organizations/" + organizationId + "/memberships",
                "{\"email\":\"guard-owner@example.com\",\"role\":\"MEMBER\"}"), 409,
                "MEMBERSHIP_ALREADY_EXISTS");
        assertProblem(owner.post(
                "/api/v1/organizations/" + organizationId + "/memberships",
                "{\"email\":\"guard-outsider@example.com\",\"role\":\"OWNER\"}"), 400,
                "OWNER_ROLE_RESERVED");
        assertProblem(owner.patch(
                "/api/v1/organizations/" + organizationId + "/memberships/"
                        + ownerMembership.path("id").asText(),
                "{\"role\":\"ADMIN\"}"), 409, "OWNER_MEMBERSHIP_IMMUTABLE");
        assertProblem(owner.delete(
                "/api/v1/organizations/" + organizationId + "/memberships/"
                        + ownerMembership.path("id").asText()), 409, "OWNER_MEMBERSHIP_IMMUTABLE");
    }

    @Test
    void rejects_unauthenticated_and_invalid_requests_without_leaking_details() throws Exception {
        SessionClient anonymous = new SessionClient();
        assertProblem(anonymous.get("/api/v1/organizations?page=0&size=10"), 401, "UNAUTHENTICATED");

        SessionClient owner = registeredClient("validation-owner@example.com");
        assertProblem(owner.get("/api/v1/organization-selection"), 404, "NO_ORGANIZATION_SELECTED");
        assertProblem(owner.post("/api/v1/organizations", "{\"name\":\" \"}"), 400, "VALIDATION_ERROR");
        assertProblem(owner.post(
                "/api/v1/organizations/00000000-0000-7000-8000-000000000000/memberships",
                "{\"email\":\"missing@example.com\",\"role\":\"MEMBER\"}"), 404,
                "ORGANIZATION_NOT_FOUND");

        String organizationId = body(owner.post(
                        "/api/v1/organizations", "{\"name\":\"Validation Workspace\"}"), 201)
                .path("id")
                .asText();
        assertProblem(owner.postWithoutCsrf(
                "/api/v1/organizations/" + organizationId + "/memberships",
                "{\"email\":\"missing@example.com\",\"role\":\"MEMBER\"}"), 403,
                "ACCESS_DENIED");
        assertProblem(owner.post(
                "/api/v1/organizations/" + organizationId + "/memberships",
                "{\"email\":\"missing@example.com\",\"role\":\"MEMBER\"}"), 404,
                "USER_NOT_FOUND");
        assertProblem(owner.patch(
                "/api/v1/organizations/" + organizationId
                        + "/memberships/00000000-0000-7000-8000-000000000000",
                "{\"role\":\"MEMBER\"}"), 404, "MEMBERSHIP_NOT_FOUND");
    }

    private SessionClient registeredClient(String email) throws Exception {
        SessionClient client = new SessionClient();
        client.refreshCsrf();
        body(client.post(
                "/api/v1/auth/registrations",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"), 201);
        body(client.post(
                "/api/v1/auth/sessions",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"), 200);
        client.refreshCsrf();
        return client;
    }

    private JsonNode body(HttpResponse<String> response, int expectedStatus) throws Exception {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body());
    }

    private void assertProblem(HttpResponse<String> response, int status, String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");
        JsonNode problem = objectMapper.readTree(response.body());
        assertThat(problem.path("code").asText()).isEqualTo(code);
        assertThat(problem.path("requestId").asText()).isNotBlank();
        assertThat(response.body()).doesNotContain(PASSWORD).doesNotContain("passwordHash");
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

        HttpResponse<String> put(String path, String body) throws Exception {
            return send("PUT", path, body, true);
        }

        HttpResponse<String> patch(String path, String body) throws Exception {
            return send("PATCH", path, body, true);
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
