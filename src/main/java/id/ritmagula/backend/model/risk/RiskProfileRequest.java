package id.ritmagula.backend.model.risk;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record RiskProfileRequest(
        @JsonProperty("age_years") int ageYears,
        @JsonProperty("sex_at_birth") String sexAtBirth,
        @JsonProperty("height_cm") BigDecimal heightCm,
        @JsonProperty("weight_kg") BigDecimal weightKg,
        @JsonProperty("waist_circumference_cm") BigDecimal waistCircumferenceCm,
        @JsonProperty("family_history_diabetes") Boolean familyHistoryDiabetes,
        Boolean hypertension,
        boolean pregnant,
        @JsonProperty("diagnosed_diabetes") boolean diagnosedDiabetes,
        @JsonProperty("taking_diabetes_medication") boolean takingDiabetesMedication
) {
}
