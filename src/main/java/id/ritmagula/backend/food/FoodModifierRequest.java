package id.ritmagula.backend.food;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record FoodModifierRequest(
        @DecimalMin("0") @DecimalMax("12") BigDecimal addedOilTeaspoons,
        @DecimalMin("0") @DecimalMax("20") BigDecimal coconutMilkTablespoons,
        @DecimalMin("0") @DecimalMax("25") BigDecimal addedSugarTeaspoons,
        @DecimalMin("0") @DecimalMax("12") BigDecimal sweetenedCondensedMilkTablespoons
) {
    public FoodModifierRequest {
        addedOilTeaspoons = zeroIfNull(addedOilTeaspoons);
        coconutMilkTablespoons = zeroIfNull(coconutMilkTablespoons);
        addedSugarTeaspoons = zeroIfNull(addedSugarTeaspoons);
        sweetenedCondensedMilkTablespoons = zeroIfNull(sweetenedCondensedMilkTablespoons);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
