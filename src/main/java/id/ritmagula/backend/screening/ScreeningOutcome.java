package id.ritmagula.backend.screening;

import id.ritmagula.backend.api.ApplicationCode;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ScreeningOutcome(
        HttpStatus httpStatus,
        ApplicationCode code,
        String message,
        ScreeningResponse data,
        List<String> warnings
) {
}
