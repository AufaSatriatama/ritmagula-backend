package id.ritmagula.backend.screening;

import id.ritmagula.backend.model.risk.RiskPredictionPayload;
import java.time.Instant;
import java.util.UUID;

public record ScreeningResponse(
        UUID auditId,
        String status,
        Instant requestedAt,
        Instant completedAt,
        RiskPredictionPayload result
) {
}
