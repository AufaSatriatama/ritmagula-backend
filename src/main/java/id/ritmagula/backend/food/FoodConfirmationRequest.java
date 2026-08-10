package id.ritmagula.backend.food;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

public record FoodConfirmationRequest(
        @NotBlank @Size(min = 8, max = 64) String analysisRequestId,
        @NotBlank @Size(min = 2, max = 100) String selectedLabel,
        @NotBlank @Pattern(regexp = "small|medium|large|custom") String portionPreset,
        @DecimalMin("10") @DecimalMax("2000") BigDecimal portionMassG,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("10") BigDecimal servings,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("1") BigDecimal eatenFraction,
        @NotNull @Valid FoodModifierRequest modifiers,
        @NotNull LocalTime mealTime,
        @AssertTrue(message = "Makanan harus dikonfirmasi sebelum disimpan.") boolean confirmedByUser
) {
    @AssertTrue(message = "portionMassG wajib diisi ketika portionPreset=custom.")
    public boolean hasRequiredCustomMass() {
        return !"custom".equals(portionPreset) || portionMassG != null;
    }
}
