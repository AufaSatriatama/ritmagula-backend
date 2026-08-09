package id.ritmagula.backend.meal;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo-sessions/{sessionId}/days/{date}/meals")
public class MealEntryController {

    private final MealEntryService service;

    public MealEntryController(MealEntryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiEnvelope<MealEntryResponse>> add(
            @PathVariable UUID sessionId,
            @PathVariable LocalDate date,
            @Valid @RequestBody MealRequest body,
            HttpServletRequest request
    ) {
        String requestId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        return ResponseEntity.ok(ApiEnvelope.success(
                requestId,
                ApplicationCode.COLLECTING_DATA,
                "Makanan manual terkonfirmasi dan tersimpan.",
                service.add(sessionId, date, body),
                List.of()
        ));
    }
}
