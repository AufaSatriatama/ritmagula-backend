package id.ritmagula.backend.timeline;

import id.ritmagula.backend.api.ApiEnvelope;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo-sessions/{sessionId}/timeline")
public class TimelineController {

    private final TimelineService service;

    public TimelineController(TimelineService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiEnvelope<TimelineResponse>> get(
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        TimelineResponse data = service.build(sessionId);
        ApplicationCode code = data.readyToScreen()
                ? ApplicationCode.READY_TO_SCREEN
                : ApplicationCode.COLLECTING_DATA;
        String message = data.readyToScreen()
                ? "Kedua window memenuhi syarat untuk meminta screening."
                : "Data belum memenuhi syarat screening.";
        String requestId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        return ResponseEntity.ok(ApiEnvelope.success(
                requestId, code, message, data,
                data.readyToScreen() ? List.of("Screening tetap bersifat riset dan bukan diagnosis.") : List.of()
        ));
    }
}
