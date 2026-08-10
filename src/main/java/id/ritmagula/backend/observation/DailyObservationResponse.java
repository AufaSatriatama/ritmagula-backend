package id.ritmagula.backend.observation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyObservationResponse(
        UUID id,
        UUID sessionId,
        LocalDate observedOn,
        List<Double> hourlyMims,
        BigDecimal wearHours,
        Integer steps,
        boolean validActivity,
        Instant updatedAt
) {
}
