package id.ritmagula.backend.profile;

import id.ritmagula.backend.api.ApiEnvelope;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo-sessions/{sessionId}/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @PutMapping
    public ResponseEntity<ApiEnvelope<ProfileResponse>> save(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ProfileRequest body,
            HttpServletRequest request
    ) {
        String requestId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        return ResponseEntity.ok(ApiEnvelope.success(
                requestId,
                ApplicationCode.COLLECTING_DATA,
                "Profil tersimpan. Lanjutkan pengumpulan data 14 hari.",
                service.save(sessionId, body),
                List.of("RitmaGula adalah screening riset, bukan diagnosis.")
        ));
    }
}
