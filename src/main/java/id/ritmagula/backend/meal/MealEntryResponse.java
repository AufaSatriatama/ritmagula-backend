package id.ritmagula.backend.meal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record MealEntryResponse(
        UUID id,
        LocalDate observedOn,
        LocalTime time,
        BigDecimal caloriesKcal,
        BigDecimal carbohydrateG,
        BigDecimal proteinG,
        BigDecimal fatG,
        BigDecimal sugarG,
        BigDecimal fiberG,
        String source,
        String sourceVersion,
        String analysisRequestId,
        String selectedLabel,
        String displayName,
        boolean confirmedByUser,
        Instant confirmedAt
) {
    static MealEntryResponse from(MealEntry meal) {
        return new MealEntryResponse(
                meal.getId(), meal.getDailyObservation().getObservedOn(), meal.getMealTime(),
                meal.getCaloriesKcal(), meal.getCarbohydrateG(), meal.getProteinG(), meal.getFatG(),
                meal.getSugarG(), meal.getFiberG(), meal.getSource(), meal.getSourceVersion(),
                meal.getAnalysisRequestId(), meal.getSelectedLabel(), meal.getDisplayName(),
                meal.isConfirmedByUser(), meal.getConfirmedAt()
        );
    }
}
