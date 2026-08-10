package id.ritmagula.backend.observation;

import id.ritmagula.backend.api.ApiEnvelope;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo-sessions/{sessionId}/days/{date}/activity")
public class ActivityController {

    private final DailyObservationService service;

    public ActivityController(DailyObservationService service) {
        this.service = service;
    }

    @PutMapping
    public ResponseEntity<ApiEnvelope<DailyObservationResponse>> save(
            @PathVariable UUID sessionId,
            @PathVariable LocalDate date,
            @Valid @RequestBody ActivityRequest body,
            HttpServletRequest request
    ) {
        DailyObservationResponse data = service.save(sessionId, date, body);
        String requestId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        List<String> warnings = data.validActivity()
                ? List.of()
                : List.of("Hari ini belum valid: butuh minimal 10 jam pakai dan 18 nilai MIMS.");
        return ResponseEntity.ok(ApiEnvelope.success(
                requestId, ApplicationCode.COLLECTING_DATA, "Aktivitas harian tersimpan.", data, warnings
        ));
    }
}
