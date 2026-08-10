package id.ritmagula.backend.api;

import java.util.List;
import org.springframework.http.HttpStatus;

public final class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ApplicationCode code;
    private final List<String> warnings;

    public ApiException(HttpStatus status, ApplicationCode code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(HttpStatus status, ApplicationCode code, String message, List<String> warnings) {
        super(message);
        this.status = status;
        this.code = code;
        this.warnings = List.copyOf(warnings);
    }

    public HttpStatus status() {
        return status;
    }

    public ApplicationCode code() {
        return code;
    }

    public List<String> warnings() {
        return warnings;
    }
}
