package id.ritmagula.backend.model.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import id.ritmagula.backend.api.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpModelHealthClientTests {

    @Test
    void preservesTypedNotReadyResponseFromHttp503() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://risk.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpModelHealthClient client = new HttpModelHealthClient("risk", builder.build());

        server.expect(requestTo("http://risk.test/v2/health"))
                .andExpect(header(RequestIdFilter.HEADER_NAME, "request-123"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "request_id": "model-request-1",
                                  "status": "not_ready",
                                  "ready": false,
                                  "service_version": "0.2.0",
                                  "clinical_use_allowed": false,
                                  "model_versions": {},
                                  "error": "missing artifact"
                                }
                                """));

        ModelServiceReadiness readiness = client.check("request-123");

        assertThat(readiness.reachable()).isTrue();
        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.status()).isEqualTo("not_ready");
        assertThat(readiness.clinicalUseAllowed()).isFalse();
        assertThat(readiness.modelVersions()).isEmpty();
        server.verify();
    }
}
