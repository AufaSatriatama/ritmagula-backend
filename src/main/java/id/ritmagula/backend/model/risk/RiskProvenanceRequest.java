package id.ritmagula.backend.model.risk;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RiskProvenanceRequest(
        String source,
        @JsonProperty("source_version") String sourceVersion,
        @JsonProperty("confirmed_by_user") boolean confirmedByUser
) {
}
