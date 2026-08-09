package id.ritmagula.backend.food;

import id.ritmagula.backend.api.ApiException;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.meal.ConfirmedFoodMeal;
import id.ritmagula.backend.meal.MealEntryResponse;
import id.ritmagula.backend.meal.MealEntryService;
import id.ritmagula.backend.model.food.ConfirmedMealPayload;
import id.ritmagula.backend.model.food.FoodAnalysisResult;
import id.ritmagula.backend.model.food.FoodAnalyzePayload;
import id.ritmagula.backend.model.food.FoodClientStatus;
import id.ritmagula.backend.model.food.FoodConfirmationCommand;
import id.ritmagula.backend.model.food.FoodConfirmationResult;
import id.ritmagula.backend.model.food.FoodConfirmPayload;
import id.ritmagula.backend.model.food.FoodModelClient;
import id.ritmagula.backend.model.food.FoodModifierCommand;
import id.ritmagula.backend.model.health.ModelHealthClient;
import id.ritmagula.backend.model.health.ModelServiceReadiness;
import id.ritmagula.backend.session.DemoSession;
import id.ritmagula.backend.session.DemoSessionService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class FoodApplicationService {

    static final long MAX_UPLOAD_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final DemoSessionService sessionService;
    private final MealEntryService mealEntryService;
    private final ModelHealthClient healthClient;
    private final FoodModelClient modelClient;
    private final ObjectMapper objectMapper;

    public FoodApplicationService(
            DemoSessionService sessionService,
            MealEntryService mealEntryService,
            @Qualifier("foodModelHealthClient") ModelHealthClient healthClient,
            FoodModelClient modelClient,
            ObjectMapper objectMapper
    ) {
        this.sessionService = sessionService;
        this.mealEntryService = mealEntryService;
        this.healthClient = healthClient;
        this.modelClient = modelClient;
        this.objectMapper = objectMapper;
    }

    public FoodAnalyzePayload analyze(
            UUID sessionId,
            String requestId,
            MultipartFile image,
            BigDecimal plateDiameterCm
    ) {
        sessionService.requireActive(sessionId);
        validateImage(image, plateDiameterCm);
        requireReady(requestId);

        FoodAnalysisResult result;
        try {
            String contentType = image.getContentType();
            result = modelClient.analyze(
                    requestId,
                    image.getBytes(),
                    safeFilename(contentType),
                    contentType,
                    plateDiameterCm
            );
        } catch (IOException exception) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Berkas gambar tidak dapat dibaca."
            );
        }
        if (result.status() != FoodClientStatus.SUCCESS) {
            throw downstreamFailure(result.status());
        }
        return result.payload();
    }

    public FoodConfirmationResponse confirm(
            UUID sessionId,
            LocalDate date,
            String requestId,
            FoodConfirmationRequest request
    ) {
        DemoSession session = sessionService.requireActive(sessionId);
        validateDate(session, date);
        requireReady(requestId);

        FoodConfirmationResult result = modelClient.confirm(requestId, toCommand(request));
        if (result.status() != FoodClientStatus.SUCCESS) {
            throw downstreamFailure(result.status());
        }

        FoodConfirmPayload payload = result.payload();
        ConfirmedMealPayload journal = payload.journalMeal();
        MealEntryResponse entry = mealEntryService.addConfirmedFood(
                sessionId,
                date,
                new ConfirmedFoodMeal(
                        journal.time(), journal.caloriesKcal(), journal.carbohydrateG(),
                        journal.proteinG(), journal.fatG(), journal.sugarG(), journal.fiberG(),
                        journal.provenance().sourceVersion(), payload.analysisRequestId(),
                        payload.selectedLabel(), payload.displayName(), writeJson(payload.modelVersions()),
                        writeJson(payload.basis())
                )
        );
        return new FoodConfirmationResponse(payload, entry);
    }

    private FoodConfirmationCommand toCommand(FoodConfirmationRequest request) {
        FoodModifierRequest modifiers = request.modifiers();
        return new FoodConfirmationCommand(
                request.analysisRequestId(), request.selectedLabel(), request.portionPreset(),
                request.portionMassG(), request.servings(), request.eatenFraction(),
                new FoodModifierCommand(
                        modifiers.addedOilTeaspoons(), modifiers.coconutMilkTablespoons(),
                        modifiers.addedSugarTeaspoons(), modifiers.sweetenedCondensedMilkTablespoons()
                ),
                request.mealTime(), true
        );
    }

    private void validateImage(MultipartFile image, BigDecimal plateDiameterCm) {
        if (image == null || image.isEmpty()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Pilih foto makanan yang akan dianalisis."
            );
        }
        if (image.getSize() > MAX_UPLOAD_BYTES) {
            throw new ApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    ApplicationCode.UPLOAD_TOO_LARGE,
                    "Ukuran foto maksimal 10 MB."
            );
        }
        if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Format foto harus JPEG, PNG, atau WebP."
            );
        }
        if (plateDiameterCm != null
                && (plateDiameterCm.compareTo(BigDecimal.TEN) < 0
                    || plateDiameterCm.compareTo(BigDecimal.valueOf(60)) > 0)) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Diameter piring harus berada pada rentang 10-60 cm."
            );
        }
    }

    private void validateDate(DemoSession session, LocalDate date) {
        if (date.isBefore(session.getObservationStartDate())
                || date.isAfter(session.getObservationStartDate().plusDays(13))) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Tanggal makanan harus berada dalam interval observasi 14 hari."
            );
        }
    }

    private void requireReady(String requestId) {
        ModelServiceReadiness readiness = healthClient.check(requestId);
        if (!readiness.ready()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ApplicationCode.MODEL_UNAVAILABLE,
                    "Food AI belum tersedia; gunakan input manual dan tidak ada nutrisi yang dibuat-buat.",
                    List.of("Foto tidak disimpan oleh backend.")
            );
        }
    }

    private ApiException downstreamFailure(FoodClientStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> new ApiException(
                    HttpStatus.UNAUTHORIZED, ApplicationCode.UNAUTHORIZED, "Konfigurasi akses Food AI ditolak."
            );
            case VALIDATION_ERROR -> new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ApplicationCode.VALIDATION_ERROR,
                    "Food AI menolak input. Periksa kembali foto dan konfirmasi."
            );
            case UPLOAD_TOO_LARGE -> new ApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE, ApplicationCode.UPLOAD_TOO_LARGE,
                    "Ukuran foto maksimal 10 MB."
            );
            case UNAVAILABLE -> new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, ApplicationCode.MODEL_UNAVAILABLE,
                    "Food AI belum tersedia; gunakan input manual."
            );
            case TIMEOUT -> new ApiException(
                    HttpStatus.GATEWAY_TIMEOUT, ApplicationCode.SERVICE_TIMEOUT,
                    "Food AI melewati batas waktu; tidak ada hasil yang disimpan."
            );
            default -> new ApiException(
                    HttpStatus.BAD_GATEWAY, ApplicationCode.NETWORK_ERROR,
                    "Respons Food AI tidak dapat digunakan; tidak ada hasil yang disimpan."
            );
        };
    }

    private String safeFilename(String contentType) {
        return switch (contentType) {
            case "image/png" -> "food-upload.png";
            case "image/webp" -> "food-upload.webp";
            default -> "food-upload.jpg";
        };
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not encode confirmed food provenance", exception);
        }
    }
}
