package id.ritmagula.backend.model.risk;

public record RiskPredictionResult(
        RiskClientStatus status,
        RiskPredictionPayload payload
) {
    public static RiskPredictionResult failure(RiskClientStatus status) {
        return new RiskPredictionResult(status, null);
    }
}
