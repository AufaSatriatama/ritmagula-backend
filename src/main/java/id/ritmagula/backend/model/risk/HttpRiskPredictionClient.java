package id.ritmagula.backend.model.risk;

import id.ritmagula.backend.api.RequestIdFilter;
import java.net.SocketTimeoutException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class HttpRiskPredictionClient implements RiskPredictionClient {

    private final RestClient restClient;

    public HttpRiskPredictionClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RiskPredictionResult predict(String requestId, RiskPredictionRequest request) {
        try {
            RiskPredictionPayload payload = restClient.post()
                    .uri("/v2/risk/predict")
                    .header(RequestIdFilter.HEADER_NAME, requestId)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 401,
                            (req, res) -> { throw new DownstreamStatusException(RiskClientStatus.UNAUTHORIZED); })
                    .onStatus(status -> status.value() == 422,
                            (req, res) -> { throw new DownstreamStatusException(RiskClientStatus.VALIDATION_ERROR); })
                    .onStatus(status -> status.value() == 503,
                            (req, res) -> { throw new DownstreamStatusException(RiskClientStatus.UNAVAILABLE); })
                    .onStatus(HttpStatusCode::isError,
                            (req, res) -> { throw new DownstreamStatusException(RiskClientStatus.NETWORK_ERROR); })
                    .body(RiskPredictionPayload.class);

            if (!validPayload(payload)) {
                return RiskPredictionResult.failure(RiskClientStatus.INVALID_RESPONSE);
            }
            return new RiskPredictionResult(RiskClientStatus.SUCCESS, payload);
        } catch (DownstreamStatusException exception) {
            return RiskPredictionResult.failure(exception.status);
        } catch (ResourceAccessException exception) {
            return RiskPredictionResult.failure(hasTimeoutCause(exception)
                    ? RiskClientStatus.TIMEOUT
                    : RiskClientStatus.NETWORK_ERROR);
        } catch (RestClientException exception) {
            return RiskPredictionResult.failure(RiskClientStatus.NETWORK_ERROR);
        }
    }

    private boolean validPayload(RiskPredictionPayload payload) {
        if (payload == null
                || payload.requestId() == null || payload.requestId().isBlank()
                || !("ok".equals(payload.status()) || "abstained".equals(payload.status()))
                || payload.clinicalUseAllowed()
                || payload.modalityQuality() == null || payload.modalityQuality().isNull()
                || payload.modelVersions().isEmpty()
                || payload.recommendation() == null || payload.recommendation().isBlank()) {
            return false;
        }
        if ("abstained".equals(payload.status())) {
            return payload.classProbabilities().isEmpty()
                    && payload.dysglycemiaProbability() == null
                    && !payload.abstentionReasons().isEmpty();
        }

        if (!payload.classProbabilities().keySet().equals(
                java.util.Set.of("normal", "prediabetes", "diabetes_range"))) {
            return false;
        }
        double probabilitySum = 0;
        for (Double probability : payload.classProbabilities().values()) {
            if (probability == null || !Double.isFinite(probability) || probability < 0 || probability > 1) {
                return false;
            }
            probabilitySum += probability;
        }
        return Math.abs(probabilitySum - 1.0) <= 0.001
                && payload.dysglycemiaProbability() != null
                && Double.isFinite(payload.dysglycemiaProbability())
                && payload.dysglycemiaProbability() >= 0
                && payload.dysglycemiaProbability() <= 1
                && payload.riskLevel() != null
                && java.util.Set.of("lebih_rendah", "meningkat", "tinggi", "tidak_pasti")
                        .contains(payload.riskLevel())
                && !payload.conformalPredictionSet().isEmpty();
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class DownstreamStatusException extends RuntimeException {
        private final RiskClientStatus status;

        private DownstreamStatusException(RiskClientStatus status) {
            this.status = status;
        }
    }
}
