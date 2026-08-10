package id.ritmagula.backend.session;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DemoSessionResponse(
        UUID id,
        String fixtureCode,
        String dataClassification,
        String lifecycleState,
        LocalDate observationStartDate,
        LocalDate observationEndDate,
        Instant consentedAt,
        Instant expiresAt
) {
    static DemoSessionResponse from(DemoSession session) {
        return new DemoSessionResponse(
                session.getId(),
                session.getFixtureCode(),
                session.getDataClassification(),
                session.getLifecycleState(),
                session.getObservationStartDate(),
                session.getObservationStartDate().plusDays(13),
                session.getConsentedAt(),
                session.getExpiresAt()
        );
    }
}
