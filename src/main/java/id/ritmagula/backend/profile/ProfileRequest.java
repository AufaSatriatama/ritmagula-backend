package id.ritmagula.backend.profile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record ProfileRequest(
        @NotNull @Min(20) @Max(60) Integer ageYears,
        @NotBlank @Pattern(regexp = "female|male") String sexAtBirth,
        @NotNull @DecimalMin("120") @DecimalMax("230") BigDecimal heightCm,
        @NotNull @DecimalMin("30") @DecimalMax("300") BigDecimal weightKg,
        @DecimalMin("50") @DecimalMax("200") BigDecimal waistCircumferenceCm,
        Boolean familyHistoryDiabetes,
        Boolean hypertension,
        boolean pregnant,
        boolean diagnosedDiabetes,
        boolean takingDiabetesMedication
) {
    public boolean outsideIntendedPopulation() {
        return pregnant || diagnosedDiabetes || takingDiabetesMedication;
    }
}
