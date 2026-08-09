package id.ritmagula.backend.model.food;

public record FoodAnalysisResult(FoodClientStatus status, FoodAnalyzePayload payload) {
    public static FoodAnalysisResult failure(FoodClientStatus status) {
        return new FoodAnalysisResult(status, null);
    }
}
