package id.ritmagula.backend.model.food;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodProvenancePayload(
        String source,
        @JsonProperty("confirmed_by_user") boolean confirmedByUser,
        @JsonProperty("source_version") String sourceVersion
) {
}
