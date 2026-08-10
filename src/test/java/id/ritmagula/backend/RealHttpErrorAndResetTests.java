package id.ritmagula.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:real-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS ritmagula_app",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
                "spring.flyway.enabled=false",
                "ritmagula.retention.cleanup-initial-delay-ms=3600000"
        }
)
class RealHttpErrorAndResetTests {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Test
    void returnsBoundedJsonForValidationNotFoundResetAndUnauthorized() throws Exception {
        HttpResponse<String> invalid = send("POST", "/api/v1/demo-sessions", """
                {"consentAccepted":false,"observationStartDate":"2026-07-20","fixtureCode":null}
                """);
        assertThat(invalid.statusCode()).isEqualTo(422);
        assertThat(invalid.body()).contains("\"code\":\"VALIDATION_ERROR\"");

        HttpResponse<String> malformed = send("POST", "/api/v1/demo-sessions", "{bad}");
        assertThat(malformed.statusCode()).isEqualTo(422);
        assertThat(malformed.body()).contains("Format request tidak valid");

        HttpResponse<String> missing = send("GET", "/api/v1/does-not-exist", null);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.body()).contains("\"code\":\"NOT_FOUND\"");

        HttpResponse<String> created = send("POST", "/api/v1/demo-sessions", """
                {"consentAccepted":true,"observationStartDate":"2026-07-20","fixtureCode":"HTTP-TEST"}
                """);
        assertThat(created.statusCode()).isEqualTo(201);
        UUID sessionId = UUID.fromString(objectMapper.readTree(created.body()).path("data").path("id").asText());

        HttpResponse<String> deleted = send("DELETE", "/api/v1/demo-sessions/" + sessionId, null);
        assertThat(deleted.statusCode()).isEqualTo(200);
        assertThat(deleted.body()).contains("\"code\":\"SESSION_RESET\"");

        HttpResponse<String> afterDelete = send("GET", "/api/v1/demo-sessions/" + sessionId + "/timeline", null);
        assertThat(afterDelete.statusCode()).isEqualTo(401);
        assertThat(afterDelete.body()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
