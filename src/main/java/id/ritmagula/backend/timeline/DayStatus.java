package id.ritmagula.backend.timeline;

import java.time.LocalDate;

public record DayStatus(
        LocalDate date,
        boolean activityEntered,
        boolean validActivity,
        int confirmedMeals
) {
}
