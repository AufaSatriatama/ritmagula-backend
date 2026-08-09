package id.ritmagula.backend.api;

import java.time.Instant;
import java.util.List;

public record ApiEnvelope<T>(
        String requestId,
        ApplicationCode code,
        String message,
        T data,
        List<String> warnings,
        Instant timestamp
) {
    public ApiEnvelope {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static <T> ApiEnvelope<T> success(
            String requestId,
            ApplicationCode code,
            String message,
            T data,
            List<String> warnings
    ) {
        return new ApiEnvelope<>(requestId, code, message, data, warnings, Instant.now());
    }
}
