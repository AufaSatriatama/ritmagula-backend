package id.ritmagula.backend.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status())
                .cacheControl(CacheControl.noStore())
                .body(new ApiEnvelope<>(
                        requestId(request),
                        exception.code(),
                        exception.getMessage(),
                        null,
                        exception.warnings(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiEnvelope<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        exception.getBindingResult().getGlobalErrors().forEach(error ->
                errors.putIfAbsent("request", error.getDefaultMessage()));

        return validationResponse(request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiEnvelope<Map<String, String>>> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return validationResponse(request, Map.of("request", "Format request tidak valid."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleUploadTooLarge(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .cacheControl(CacheControl.noStore())
                .body(new ApiEnvelope<>(
                        requestId(request),
                        ApplicationCode.UPLOAD_TOO_LARGE,
                        "Ukuran foto maksimal 10 MB.",
                        null,
                        List.of("Foto tidak disimpan oleh backend."),
                        Instant.now()
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .cacheControl(CacheControl.noStore())
                .body(new ApiEnvelope<>(
                        requestId(request),
                        ApplicationCode.NOT_FOUND,
                        "Endpoint API tidak ditemukan.",
                        null,
                        List.of(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiEnvelope<Void>> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .cacheControl(CacheControl.noStore())
                .body(new ApiEnvelope<>(
                        requestId(request),
                        ApplicationCode.INTERNAL_ERROR,
                        "Terjadi kesalahan internal. Data tidak diproses lebih lanjut.",
                        null,
                        List.of(),
                        Instant.now()
                ));
    }

    private ResponseEntity<ApiEnvelope<Map<String, String>>> validationResponse(
            HttpServletRequest request,
            Map<String, String> errors
    ) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .cacheControl(CacheControl.noStore())
                .body(new ApiEnvelope<>(
                        requestId(request),
                        ApplicationCode.VALIDATION_ERROR,
                        "Periksa kembali data yang dimasukkan.",
                        Map.copyOf(errors),
                        List.of(),
                        Instant.now()
                ));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
    }
}
