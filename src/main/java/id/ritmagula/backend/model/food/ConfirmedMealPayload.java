package id.ritmagula.backend.model.food;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfirmedMealPayload(
        LocalTime time,
        @JsonProperty("calories_kcal") BigDecimal caloriesKcal,
        @JsonProperty("carbohydrate_g") BigDecimal carbohydrateG,
        @JsonProperty("protein_g") BigDecimal proteinG,
        @JsonProperty("fat_g") BigDecimal fatG,
        @JsonProperty("sugar_g") BigDecimal sugarG,
        @JsonProperty("fiber_g") BigDecimal fiberG,
        FoodProvenancePayload provenance
) {
}
