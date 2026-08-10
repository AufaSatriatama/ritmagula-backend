package id.ritmagula.backend.model.risk;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RiskDayRequest(
        LocalDate date,
        @JsonProperty("hourly_mims") List<Double> hourlyMims,
        @JsonProperty("wear_hours") BigDecimal wearHours,
        Integer steps,
        List<RiskMealRequest> meals
) {
}
