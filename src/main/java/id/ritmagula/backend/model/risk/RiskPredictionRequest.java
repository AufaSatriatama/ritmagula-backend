package id.ritmagula.backend.model.risk;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RiskPredictionRequest(
        RiskProfileRequest profile,
        List<RiskDayRequest> days,
        @JsonProperty("observation_window_days") int observationWindowDays,
        @JsonProperty("client_reference") String clientReference
) {
}
