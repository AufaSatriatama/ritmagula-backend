package id.ritmagula.backend.model.health;

import id.ritmagula.backend.api.RequestIdFilter;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class HttpModelHealthClient implements ModelHealthClient {

    private final String serviceName;
    private final RestClient restClient;

    public HttpModelHealthClient(String serviceName, RestClient restClient) {
        this.serviceName = serviceName;
        this.restClient = restClient;
    }

    @Override
    public ModelServiceReadiness check(String requestId) {
        try {
            ModelHealthPayload payload = restClient.get()
                    .uri("/v2/health")
                    .header(RequestIdFilter.HEADER_NAME, requestId)
                    .retrieve()
                    .onStatus(status -> status.value() == 503, (request, response) -> {
                        // HTTP 503 is a valid, typed not-ready state from the model services.
                    })
                    .body(ModelHealthPayload.class);

            if (payload == null) {
                return invalidResponse();
            }

            boolean safeReady = payload.ready() && !payload.clinicalUseAllowed();

            return new ModelServiceReadiness(
                    serviceName,
                    true,
                    safeReady,
                    safeReady || !payload.ready() ? payload.status() : "invalid_response",
                    payload.serviceVersion(),
                    payload.clinicalUseAllowed(),
                    payload.modelVersions() == null ? Map.of() : payload.modelVersions(),
                    payload.error()
            );
        } catch (RestClientException exception) {
            return ModelServiceReadiness.unreachable(serviceName);
        }
    }

    private ModelServiceReadiness invalidResponse() {
        return new ModelServiceReadiness(
                serviceName,
                true,
                false,
                "invalid_response",
                null,
                false,
                Map.of(),
                "invalid_response"
        );
    }
}
