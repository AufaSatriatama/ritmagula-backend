package id.ritmagula.backend.model.food;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodAnalyzePayload(
        @JsonProperty("request_id") String requestId,
        String status,
        @JsonProperty("clinical_use_allowed") boolean clinicalUseAllowed,
        JsonNode quality,
        JsonNode mask,
        @JsonProperty("bounding_box") JsonNode boundingBox,
        List<JsonNode> candidates,
        @JsonProperty("unknown_food") Boolean unknownFood,
        JsonNode nutrition,
        @JsonProperty("requires_user_confirmation") boolean requiresUserConfirmation,
        @JsonProperty("model_versions") Map<String, String> modelVersions,
        @JsonProperty("abstention_reasons") List<String> abstentionReasons,
        List<String> warnings,
        @JsonProperty("usage_mode") String usageMode,
        @JsonProperty("evidence_grade") String evidenceGrade,
        @JsonProperty("portion_suggestions") List<JsonNode> portionSuggestions,
        @JsonProperty("confirmation_questions") List<JsonNode> confirmationQuestions,
        @JsonProperty("nutrition_profile_version") String nutritionProfileVersion
) {
    public FoodAnalyzePayload {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        modelVersions = modelVersions == null ? Map.of() : Map.copyOf(modelVersions);
        abstentionReasons = abstentionReasons == null ? List.of() : List.copyOf(abstentionReasons);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        portionSuggestions = portionSuggestions == null ? List.of() : List.copyOf(portionSuggestions);
        confirmationQuestions = confirmationQuestions == null ? List.of() : List.copyOf(confirmationQuestions);
    }
}
