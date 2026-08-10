package id.ritmagula.backend.timeline;

import java.time.LocalDate;

public record WindowStatus(
        int window,
        LocalDate startDate,
        LocalDate endDate,
        int validActivityDays,
        int confirmedMealDays,
        boolean usable
) {
}
