package id.ritmagula.backend.screening;

import id.ritmagula.backend.api.ApiEnvelope;
import id.ritmagula.backend.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo-sessions/{sessionId}/screenings")
public class ScreeningController {

    private final ScreeningService service;

    public ScreeningController(ScreeningService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiEnvelope<ScreeningResponse>> create(
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        String requestId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        ScreeningOutcome outcome = service.screen(sessionId, requestId);
        ApiEnvelope<ScreeningResponse> body = new ApiEnvelope<>(
                requestId,
                outcome.code(),
                outcome.message(),
                outcome.data(),
                outcome.warnings(),
                Instant.now()
        );
        return ResponseEntity.status(outcome.httpStatus()).cacheControl(CacheControl.noStore()).body(body);
    }
}
