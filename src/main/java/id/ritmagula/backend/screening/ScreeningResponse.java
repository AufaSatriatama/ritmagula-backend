package id.ritmagula.backend.screening;

import java.time.Instant;
import java.util.UUID;

public record ScreeningResponse(
        UUID auditId,
        String status,
        Instant requestedAt,
        Instant completedAt,
        ScreeningResultResponse result
) {
}
