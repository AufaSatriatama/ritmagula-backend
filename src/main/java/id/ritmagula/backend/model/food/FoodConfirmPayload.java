package id.ritmagula.backend.model.food;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodConfirmPayload(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("analysis_request_id") String analysisRequestId,
        String status,
        @JsonProperty("clinical_use_allowed") boolean clinicalUseAllowed,
        @JsonProperty("selected_label") String selectedLabel,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("portion_mass") JsonNode portionMass,
        JsonNode nutrition,
        JsonNode basis,
        @JsonProperty("journal_meal") ConfirmedMealPayload journalMeal,
        @JsonProperty("model_versions") Map<String, String> modelVersions,
        List<String> warnings
) {
    public FoodConfirmPayload {
        modelVersions = modelVersions == null ? Map.of() : Map.copyOf(modelVersions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
