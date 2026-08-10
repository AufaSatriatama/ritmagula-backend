package id.ritmagula.backend.meal;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ConfirmedFoodMeal(
        LocalTime time,
        BigDecimal caloriesKcal,
        BigDecimal carbohydrateG,
        BigDecimal proteinG,
        BigDecimal fatG,
        BigDecimal sugarG,
        BigDecimal fiberG,
        String sourceVersion,
        String analysisRequestId,
        String selectedLabel,
        String displayName,
        String modelVersionsJson,
        String nutritionBasisJson
) {
}
