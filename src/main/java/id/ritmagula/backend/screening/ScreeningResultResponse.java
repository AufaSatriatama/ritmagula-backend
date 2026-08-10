package id.ritmagula.backend.screening;

import id.ritmagula.backend.model.risk.RiskPredictionPayload;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public record ScreeningResultResponse(
        String requestId,
        String status,
        Map<String, Double> classProbabilities,
        Double dysglycemiaProbability,
        String riskLevel,
        List<String> conformalPredictionSet,
        JsonNode modalityQuality,
        JsonNode drivingFactors,
        Map<String, Object> uncertainty,
        Map<String, String> modelVersions,
        String recommendation,
        List<String> warnings,
        List<String> abstentionReasons,
        boolean clinicalUseAllowed
) {
    static ScreeningResultResponse from(RiskPredictionPayload payload) {
        return new ScreeningResultResponse(
                payload.requestId(), payload.status(), payload.classProbabilities(),
                payload.dysglycemiaProbability(), payload.riskLevel(), payload.conformalPredictionSet(),
                payload.modalityQuality(), payload.drivingFactors(), payload.uncertainty(),
                payload.modelVersions(), payload.recommendation(), payload.warnings(),
                payload.abstentionReasons(), payload.clinicalUseAllowed()
        );
    }
}
