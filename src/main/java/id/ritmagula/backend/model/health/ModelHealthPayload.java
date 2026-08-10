package id.ritmagula.backend.model.health;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record ModelHealthPayload(
        @JsonProperty("request_id") String requestId,
        String status,
        boolean ready,
        @JsonProperty("service_version") String serviceVersion,
        @JsonProperty("clinical_use_allowed") boolean clinicalUseAllowed,
        @JsonProperty("model_versions") Map<String, String> modelVersions,
        String error
) {
}
