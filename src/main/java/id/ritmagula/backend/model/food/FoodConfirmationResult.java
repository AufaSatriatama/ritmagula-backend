package id.ritmagula.backend.model.food;

public record FoodConfirmationResult(FoodClientStatus status, FoodConfirmPayload payload) {
    public static FoodConfirmationResult failure(FoodClientStatus status) {
        return new FoodConfirmationResult(status, null);
    }
}
