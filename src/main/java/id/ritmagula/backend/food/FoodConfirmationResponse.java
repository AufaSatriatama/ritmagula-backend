package id.ritmagula.backend.food;

import id.ritmagula.backend.meal.MealEntryResponse;
import id.ritmagula.backend.model.food.FoodConfirmPayload;

public record FoodConfirmationResponse(
        FoodConfirmPayload confirmation,
        MealEntryResponse journalEntry
) {
}
