package id.ritmagula.backend.model.food;

import id.ritmagula.backend.api.RequestIdFilter;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class HttpFoodModelClient implements FoodModelClient {

    private final RestClient restClient;

    public HttpFoodModelClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public FoodAnalysisResult analyze(
            String requestId,
            byte[] image,
            String filename,
            String contentType,
            BigDecimal plateDiameterCm
    ) {
        HttpHeaders imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(MediaType.parseMediaType(contentType));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new HttpEntity<>(new NamedByteArrayResource(image, filename), imageHeaders));
        if (plateDiameterCm != null) {
            body.add("plate_diameter_cm", plateDiameterCm.toPlainString());
        }

        try {
            FoodAnalyzePayload payload = restClient.post()
                    .uri("/v2/food/analyze")
                    .header(RequestIdFilter.HEADER_NAME, requestId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 401,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.UNAUTHORIZED); })
                    .onStatus(status -> status.value() == 413,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.UPLOAD_TOO_LARGE); })
                    .onStatus(status -> status.value() == 422,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.VALIDATION_ERROR); })
                    .onStatus(status -> status.value() == 503,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.UNAVAILABLE); })
                    .onStatus(HttpStatusCode::isError,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.NETWORK_ERROR); })
                    .body(FoodAnalyzePayload.class);
            if (!validAnalysis(payload)) {
                return FoodAnalysisResult.failure(FoodClientStatus.INVALID_RESPONSE);
            }
            return new FoodAnalysisResult(FoodClientStatus.SUCCESS, payload);
        } catch (DownstreamStatusException exception) {
            return FoodAnalysisResult.failure(exception.status);
        } catch (ResourceAccessException exception) {
            return FoodAnalysisResult.failure(hasTimeoutCause(exception)
                    ? FoodClientStatus.TIMEOUT : FoodClientStatus.NETWORK_ERROR);
        } catch (RestClientException exception) {
            return FoodAnalysisResult.failure(FoodClientStatus.NETWORK_ERROR);
        }
    }

    @Override
    public FoodConfirmationResult confirm(String requestId, FoodConfirmationCommand command) {
        try {
            FoodConfirmPayload payload = restClient.post()
                    .uri("/v2/food/confirm")
                    .header(RequestIdFilter.HEADER_NAME, requestId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .onStatus(status -> status.value() == 401,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.UNAUTHORIZED); })
                    .onStatus(status -> status.value() == 422,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.VALIDATION_ERROR); })
                    .onStatus(status -> status.value() == 503,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.UNAVAILABLE); })
                    .onStatus(HttpStatusCode::isError,
                            (req, res) -> { throw new DownstreamStatusException(FoodClientStatus.NETWORK_ERROR); })
                    .body(FoodConfirmPayload.class);
            if (!validConfirmation(payload, command)) {
                return FoodConfirmationResult.failure(FoodClientStatus.INVALID_RESPONSE);
            }
            return new FoodConfirmationResult(FoodClientStatus.SUCCESS, payload);
        } catch (DownstreamStatusException exception) {
            return FoodConfirmationResult.failure(exception.status);
        } catch (ResourceAccessException exception) {
            return FoodConfirmationResult.failure(hasTimeoutCause(exception)
                    ? FoodClientStatus.TIMEOUT : FoodClientStatus.NETWORK_ERROR);
        } catch (RestClientException exception) {
            return FoodConfirmationResult.failure(FoodClientStatus.NETWORK_ERROR);
        }
    }

    private boolean validAnalysis(FoodAnalyzePayload payload) {
        return payload != null
                && payload.requestId() != null && !payload.requestId().isBlank()
                && ("ok".equals(payload.status()) || "partial".equals(payload.status())
                    || "abstained".equals(payload.status()))
                && !payload.clinicalUseAllowed()
                && payload.requiresUserConfirmation()
                && payload.quality() != null && !payload.quality().isNull();
    }

    private boolean validConfirmation(FoodConfirmPayload payload, FoodConfirmationCommand command) {
        return payload != null
                && "confirmed".equals(payload.status())
                && !payload.clinicalUseAllowed()
                && payload.requestId() != null && !payload.requestId().isBlank()
                && command.analysisRequestId().equals(payload.analysisRequestId())
                && command.selectedLabel().equals(payload.selectedLabel())
                && payload.displayName() != null && !payload.displayName().isBlank()
                && payload.portionMass() != null && !payload.portionMass().isNull()
                && payload.nutrition() != null && !payload.nutrition().isNull()
                && payload.basis() != null && !payload.basis().isNull()
                && payload.journalMeal() != null
                && command.mealTime().equals(payload.journalMeal().time())
                && payload.journalMeal().provenance() != null
                && payload.journalMeal().provenance().confirmedByUser()
                && "food_cv".equals(payload.journalMeal().provenance().source())
                && payload.journalMeal().provenance().sourceVersion() != null
                && !payload.journalMeal().provenance().sourceVersion().isBlank()
                && validNutrients(payload.journalMeal())
                && !payload.modelVersions().isEmpty();
    }

    private boolean validNutrients(ConfirmedMealPayload meal) {
        return inRange(meal.caloriesKcal(), 0, 5000)
                && inRange(meal.carbohydrateG(), 0, 800)
                && inRange(meal.proteinG(), 0, 500)
                && inRange(meal.fatG(), 0, 500)
                && optionalInRange(meal.sugarG(), 0, 500)
                && optionalInRange(meal.fiberG(), 0, 200);
    }

    private boolean inRange(BigDecimal value, int minimum, int maximum) {
        return value != null
                && value.compareTo(BigDecimal.valueOf(minimum)) >= 0
                && value.compareTo(BigDecimal.valueOf(maximum)) <= 0;
    }

    private boolean optionalInRange(BigDecimal value, int minimum, int maximum) {
        return value == null || inRange(value, minimum, maximum);
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

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private static final class DownstreamStatusException extends RuntimeException {
        private final FoodClientStatus status;

        private DownstreamStatusException(FoodClientStatus status) {
            this.status = status;
        }
    }
}
