package dev.portoduque.saas;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portoduque.saas.shared.api.ApiPaths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
        })
@Import(ApiConventionsIntegrationTest.ContractTestController.class)
@ExtendWith(OutputCaptureExtension.class)
class ApiConventionsIntegrationTest {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returns_deterministic_openapi_contract_for_versioned_api() throws Exception {
        var first = get("/v3/api-docs");
        var second = get("/v3/api-docs");

        assertThat(first.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(first.body()).isEqualTo(second.body());

        JsonNode contract = objectMapper.readTree(first.body());
        assertThat(contract.path("info").path("title").asText()).isEqualTo("SaaS-Skeleton API");
        assertThat(contract.path("info").path("version").asText()).isEqualTo("v1");
        assertThat(contract.path("paths").propertyNames())
                .allMatch(path -> path.startsWith("/api/v1/"));
        assertThat(contract.path("paths").has("/api/v1/contract-tests")).isTrue();
    }

    @Test
    void returns_standard_problem_details_for_invalid_dto() throws Exception {
        var response = post("/api/v1/contract-tests", "{\"name\":\"\",\"password\":\"never-log-me\"}", null);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");

        JsonNode problem = objectMapper.readTree(response.body());
        assertThat(problem.path("status").asInt()).isEqualTo(400);
        assertThat(problem.path("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(problem.path("detail").asText()).isEqualTo("One or more request fields are invalid.");
        assertThat(problem.path("instance").asText()).isEqualTo("/api/v1/contract-tests");
        assertThat(problem.path("requestId").asText()).isNotBlank();
        assertThat(problem.path("violations").get(0).path("field").asText()).isEqualTo("name");
        assertThat(problem.path("violations").get(0).path("message").asText()).isEqualTo("must not be blank");
        assertThat(response.body()).doesNotContain("never-log-me");
    }

    @Test
    void reuses_safe_request_id_and_replaces_unsafe_values() throws Exception {
        var accepted = post("/api/v1/contract-tests", "{\"name\":\"valid\"}", "trace-123_A");
        var replaced = post("/api/v1/contract-tests", "{\"name\":\"valid\"}", "x".repeat(129));

        assertThat(accepted.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        assertThat(accepted.headers().firstValue(REQUEST_ID_HEADER)).contains("trace-123_A");
        assertThat(replaced.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        assertThat(replaced.headers().firstValue(REQUEST_ID_HEADER).orElseThrow())
                .matches(value -> {
                    try {
                        UUID.fromString(value);
                        return true;
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                });
    }

    @Test
    void exposes_only_summary_health_information() throws Exception {
        var response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode health = objectMapper.readTree(response.body());
        assertThat(health.path("status").asText()).isEqualTo("UP");
        assertThat(health.has("components")).isFalse();
    }

    @Test
    void standardizes_not_found_and_method_parameter_validation_errors() throws Exception {
        var notFound = get("/api/v1/missing");
        var invalidParameter = get("/api/v1/contract-tests/parameters?limit=0");

        assertThat(notFound.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        JsonNode missingProblem = objectMapper.readTree(notFound.body());
        assertThat(missingProblem.path("code").asText()).isEqualTo("NOT_FOUND");
        assertThat(missingProblem.path("detail").asText()).isEqualTo("The requested resource was not found.");
        assertThat(missingProblem.path("requestId").asText()).isNotBlank();

        assertThat(invalidParameter.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        JsonNode validationProblem = objectMapper.readTree(invalidParameter.body());
        assertThat(validationProblem.path("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(validationProblem.path("violations").get(0).path("field").asText()).isEqualTo("limit");
    }

    @Test
    void does_not_log_request_bodies_headers_or_exception_messages(CapturedOutput output) throws Exception {
        var response = post(
                "/api/v1/contract-tests/failure",
                "{\"name\":\"valid\",\"password\":\"body-secret-value\"}",
                "safe-request-id");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        JsonNode problem = objectMapper.readTree(response.body());
        assertThat(problem.path("code").asText()).isEqualTo("INTERNAL_ERROR");
        assertThat(problem.path("detail").asText()).isEqualTo("An unexpected error occurred.");
        assertThat(response.body()).doesNotContain("exception-secret-value");
        assertThat(output.getAll())
                .contains("\"requestId\":\"safe-request-id\"")
                .doesNotContain("body-secret-value")
                .doesNotContain("header-secret-value")
                .doesNotContain("exception-secret-value");
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).GET().build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String requestId) throws Exception {
        var builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer header-secret-value")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (requestId != null) {
            builder.header(REQUEST_ID_HEADER, requestId);
        }
        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    @RestController
    @RequestMapping(ApiPaths.V1 + "/contract-tests")
    static class ContractTestController {

        @PostMapping
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void validate(@Valid @RequestBody ContractRequest request) {}

        @PostMapping("/failure")
        void fail(@Valid @RequestBody ContractRequest request) {
            throw new IllegalStateException("exception-secret-value");
        }

        @GetMapping("/parameters")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void validateParameter(@RequestParam @Min(1) int limit) {}
    }

    record ContractRequest(@NotBlank String name, String password) {}
}
