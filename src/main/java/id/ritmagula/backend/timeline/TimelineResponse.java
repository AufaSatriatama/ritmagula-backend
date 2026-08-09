package id.ritmagula.backend.timeline;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TimelineResponse(
        UUID sessionId,
        LocalDate observationStartDate,
        LocalDate observationEndDate,
        boolean profileComplete,
        List<DayStatus> days,
        List<WindowStatus> windows,
        boolean readyToScreen,
        List<String> requiredActions
) {
}
