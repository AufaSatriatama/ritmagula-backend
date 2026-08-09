package id.ritmagula.backend.profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
        UUID sessionId,
        int ageYears,
        String sexAtBirth,
        BigDecimal heightCm,
        BigDecimal weightKg,
        Boolean familyHistoryDiabetes,
        Boolean hypertension,
        boolean eligible,
        Instant eligibilityConfirmedAt,
        Instant updatedAt
) {
    static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.getSessionId(), profile.getAgeYears(), profile.getSexAtBirth(),
                profile.getHeightCm(), profile.getWeightKg(), profile.getFamilyHistoryDiabetes(),
                profile.getHypertension(), true, profile.getEligibilityConfirmedAt(), profile.getUpdatedAt()
        );
    }
}
