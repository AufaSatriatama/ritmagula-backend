package id.ritmagula.backend.model.risk;

public interface RiskPredictionClient {

    RiskPredictionResult predict(String requestId, RiskPredictionRequest request);
}
