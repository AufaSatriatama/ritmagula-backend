package id.ritmagula.backend.system;

import id.ritmagula.backend.api.ApiEnvelope;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemReadinessController {

    private final SystemReadinessService readinessService;

    public SystemReadinessController(SystemReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiEnvelope<SystemReadinessData>> readiness(HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        SystemReadinessData data = readinessService.check(requestId);

        ApiEnvelope<SystemReadinessData> body = ApiEnvelope.success(
                requestId,
                ApplicationCode.SYSTEM_READY,
                "Status layanan berhasil diperiksa.",
                data,
                readinessService.warnings(data)
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
