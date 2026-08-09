package id.ritmagula.backend.meal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

public record MealRequest(
        @NotNull LocalTime time,
        @NotNull @DecimalMin("0") @DecimalMax("5000") BigDecimal caloriesKcal,
        @NotNull @DecimalMin("0") @DecimalMax("800") BigDecimal carbohydrateG,
        @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal proteinG,
        @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal fatG,
        @DecimalMin("0") @DecimalMax("500") BigDecimal sugarG,
        @DecimalMin("0") @DecimalMax("200") BigDecimal fiberG,
        @AssertTrue(message = "Makanan harus dikonfirmasi sebelum disimpan.") boolean confirmedByUser,
        @NotBlank @Size(max = 100) String sourceVersion
) {
}
