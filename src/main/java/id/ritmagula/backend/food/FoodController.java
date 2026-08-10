package id.ritmagula.backend.food;

import id.ritmagula.backend.api.ApiEnvelope;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.api.RequestIdFilter;
import id.ritmagula.backend.model.food.FoodAnalyzePayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/demo-sessions/{sessionId}")
public class FoodController {

    private final FoodApplicationService service;

    public FoodController(FoodApplicationService service) {
        this.service = service;
    }

    @PostMapping(value = "/food-analyses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<FoodAnalyzePayload>> analyze(
            @PathVariable UUID sessionId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(name = "plateDiameterCm", required = false) BigDecimal plateDiameterCm,
            HttpServletRequest request
    ) {
        String requestId = requestId(request);
        FoodAnalyzePayload result = service.analyze(sessionId, requestId, image, plateDiameterCm);
        ApplicationCode code = switch (result.status()) {
            case "ok" -> ApplicationCode.CONFIRMATION_REQUIRED;
            case "partial" -> ApplicationCode.PARTIAL_CONFIRMATION_REQUIRED;
            default -> ApplicationCode.MANUAL_ENTRY_REQUIRED;
        };
        String message = "abstained".equals(result.status())
                ? "Food AI memilih abstain. Gunakan input manual."
                : "Periksa label dan porsi, lalu konfirmasi sebelum masuk jurnal.";
        List<String> warnings = new ArrayList<>(result.warnings());
        warnings.add("Saran Food AI bersifat eksperimental dan belum masuk jurnal.");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.success(requestId, code, message, result, List.copyOf(warnings)));
    }

    @PostMapping(value = "/days/{date}/food-confirmations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiEnvelope<FoodConfirmationResponse>> confirm(
            @PathVariable UUID sessionId,
            @PathVariable LocalDate date,
            @Valid @RequestBody FoodConfirmationRequest body,
            HttpServletRequest request
    ) {
        String requestId = requestId(request);
        FoodConfirmationResponse result = service.confirm(sessionId, date, requestId, body);
        List<String> warnings = new ArrayList<>(result.confirmation().warnings());
        warnings.add("Nutrisi adalah estimasi interval berbasis TKPI/analog setelah konfirmasi pengguna.");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.success(
                        requestId,
                        ApplicationCode.COLLECTING_DATA,
                        "Makanan terkonfirmasi dan disimpan ke jurnal dengan provenance.",
                        result,
                        List.copyOf(warnings)
                ));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
    }
}
