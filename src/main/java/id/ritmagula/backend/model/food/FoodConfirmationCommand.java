package id.ritmagula.backend.model.food;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalTime;

public record FoodConfirmationCommand(
        @JsonProperty("analysis_request_id") String analysisRequestId,
        @JsonProperty("selected_label") String selectedLabel,
        @JsonProperty("portion_preset") String portionPreset,
        @JsonProperty("portion_mass_g") BigDecimal portionMassG,
        BigDecimal servings,
        @JsonProperty("eaten_fraction") BigDecimal eatenFraction,
        FoodModifierCommand modifiers,
        @JsonProperty("meal_time") LocalTime mealTime,
        @JsonProperty("confirmed_by_user") boolean confirmedByUser
) {
}
