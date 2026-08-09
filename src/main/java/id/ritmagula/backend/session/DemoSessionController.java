package id.ritmagula.backend.session;

import id.ritmagula.backend.api.ApiEnvelope;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo-sessions")
public class DemoSessionController {

    private final DemoSessionService service;

    public DemoSessionController(DemoSessionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiEnvelope<DemoSessionResponse>> create(
            @Valid @RequestBody CreateDemoSessionRequest body,
            HttpServletRequest request
    ) {
        DemoSessionResponse data = service.create(body);
        String requestId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        return ResponseEntity.created(URI.create("/api/v1/demo-sessions/" + data.id()))
                .body(ApiEnvelope.success(
                        requestId,
                        ApplicationCode.COLLECTING_DATA,
                        "Sesi demo aktif selama maksimal 24 jam.",
                        data,
                        List.of("Gunakan hanya data fiktif/non-personal untuk demo lokal.")
                ));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID sessionId) {
        service.delete(sessionId);
        return ResponseEntity.noContent().build();
    }
}
