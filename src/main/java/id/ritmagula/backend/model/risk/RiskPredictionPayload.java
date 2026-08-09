package id.ritmagula.backend.model.risk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskPredictionPayload(
        @JsonProperty("request_id") String requestId,
        String status,
        @JsonProperty("class_probabilities") Map<String, Double> classProbabilities,
        @JsonProperty("dysglycemia_probability") Double dysglycemiaProbability,
        @JsonProperty("risk_level") String riskLevel,
        @JsonProperty("conformal_prediction_set") List<String> conformalPredictionSet,
        @JsonProperty("modality_quality") JsonNode modalityQuality,
        @JsonProperty("driving_factors") JsonNode drivingFactors,
        Map<String, Object> uncertainty,
        @JsonProperty("model_versions") Map<String, String> modelVersions,
        String recommendation,
        List<String> warnings,
        @JsonProperty("abstention_reasons") List<String> abstentionReasons,
        @JsonProperty("clinical_use_allowed") boolean clinicalUseAllowed
) {
    public RiskPredictionPayload {
        classProbabilities = classProbabilities == null ? Map.of() : Map.copyOf(classProbabilities);
        conformalPredictionSet = conformalPredictionSet == null ? List.of() : List.copyOf(conformalPredictionSet);
        uncertainty = uncertainty == null ? Map.of() : Map.copyOf(uncertainty);
        modelVersions = modelVersions == null ? Map.of() : Map.copyOf(modelVersions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        abstentionReasons = abstentionReasons == null ? List.of() : List.copyOf(abstentionReasons);
    }
}
