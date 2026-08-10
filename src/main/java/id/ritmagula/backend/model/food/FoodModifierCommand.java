package id.ritmagula.backend.model.food;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record FoodModifierCommand(
        @JsonProperty("added_oil_teaspoons") BigDecimal addedOilTeaspoons,
        @JsonProperty("coconut_milk_tablespoons") BigDecimal coconutMilkTablespoons,
        @JsonProperty("added_sugar_teaspoons") BigDecimal addedSugarTeaspoons,
        @JsonProperty("sweetened_condensed_milk_tablespoons") BigDecimal sweetenedCondensedMilkTablespoons
) {
}
