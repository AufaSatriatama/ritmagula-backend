package id.ritmagula.backend.observation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ActivityRequest(
        @NotNull @Size(min = 24, max = 24, message = "MIMS harus berisi tepat 24 nilai per jam.")
        List<Double> hourlyMims,
        @NotNull @DecimalMin("0") @DecimalMax("24") BigDecimal wearHours,
        @Min(0) @Max(100000) Integer steps
) {
}
