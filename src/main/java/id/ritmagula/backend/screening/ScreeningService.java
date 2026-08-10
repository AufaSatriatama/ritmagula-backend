package id.ritmagula.backend.screening;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import id.ritmagula.backend.api.ApiException;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.meal.MealEntry;
import id.ritmagula.backend.meal.MealEntryService;
import id.ritmagula.backend.model.health.ModelHealthClient;
import id.ritmagula.backend.model.health.ModelServiceReadiness;
import id.ritmagula.backend.model.risk.RiskClientStatus;
import id.ritmagula.backend.model.risk.RiskDayRequest;
import id.ritmagula.backend.model.risk.RiskMealRequest;
import id.ritmagula.backend.model.risk.RiskPredictionClient;
import id.ritmagula.backend.model.risk.RiskPredictionPayload;
import id.ritmagula.backend.model.risk.RiskPredictionRequest;
import id.ritmagula.backend.model.risk.RiskPredictionResult;
import id.ritmagula.backend.model.risk.RiskProfileRequest;
import id.ritmagula.backend.model.risk.RiskProvenanceRequest;
import id.ritmagula.backend.observation.DailyObservation;
import id.ritmagula.backend.observation.DailyObservationService;
import id.ritmagula.backend.profile.Profile;
import id.ritmagula.backend.profile.ProfileRepository;
import id.ritmagula.backend.session.DemoSession;
import id.ritmagula.backend.session.DemoSessionService;
import id.ritmagula.backend.timeline.TimelineResponse;
import id.ritmagula.backend.timeline.TimelineService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScreeningService {

    private final DemoSessionService sessionService;
    private final ProfileRepository profileRepository;
    private final DailyObservationService observationService;
    private final MealEntryService mealService;
    private final TimelineService timelineService;
    private final ModelHealthClient riskHealthClient;
    private final RiskPredictionClient predictionClient;
    private final ScreeningAuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    public ScreeningService(
            DemoSessionService sessionService,
            ProfileRepository profileRepository,
            DailyObservationService observationService,
            MealEntryService mealService,
            TimelineService timelineService,
            @Qualifier("riskModelHealthClient") ModelHealthClient riskHealthClient,
            RiskPredictionClient predictionClient,
            ScreeningAuditRepository auditRepository,
            ObjectMapper objectMapper
    ) {
        this.sessionService = sessionService;
        this.profileRepository = profileRepository;
        this.observationService = observationService;
        this.mealService = mealService;
        this.timelineService = timelineService;
        this.riskHealthClient = riskHealthClient;
        this.predictionClient = predictionClient;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ScreeningOutcome screen(UUID sessionId, String requestId) {
        DemoSession session = sessionService.requireActive(sessionId);
        TimelineResponse timeline = timelineService.build(sessionId);
        if (!timeline.readyToScreen()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.COLLECTING_DATA,
                    "Data belum memenuhi syarat screening.",
                    timeline.requiredActions()
            );
        }

        ScreeningAudit audit = auditRepository.save(new ScreeningAudit(session, requestId, clock.instant()));
        ModelServiceReadiness readiness = riskHealthClient.check(requestId);
        if (!readiness.ready()) {
            audit.complete("MODEL_UNAVAILABLE", "not_ready", writeJson(readiness.modelVersions()), clock.instant());
            auditRepository.save(audit);
            return outcome(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ApplicationCode.MODEL_UNAVAILABLE,
                    "Model risiko belum tersedia; tidak ada hasil screening yang dibuat.",
                    audit,
                    null,
                    List.of("Data harian tetap tersimpan. Coba kembali setelah layanan model pulih.")
            );
        }

        RiskPredictionResult result = predictionClient.predict(requestId, buildRequest(sessionId, requestId));
        if (result.status() != RiskClientStatus.SUCCESS) {
            return failureOutcome(audit, result.status());
        }

        RiskPredictionPayload payload = result.payload();
        String applicationStatus = "ok".equals(payload.status()) ? "RESULT_AVAILABLE" : "ABSTAINED";
        ApplicationCode code = "ok".equals(payload.status())
                ? ApplicationCode.RESULT_AVAILABLE
                : ApplicationCode.ABSTAINED;
        audit.complete(applicationStatus, payload.status(), writeJson(payload.modelVersions()), clock.instant());
        auditRepository.save(audit);

        List<String> warnings = new ArrayList<>(payload.warnings());
        warnings.add("Hasil adalah sinyal screening riset, bukan diagnosis.");
        return outcome(HttpStatus.OK, code,
                code == ApplicationCode.RESULT_AVAILABLE
                        ? "Hasil screening riset tersedia."
                        : "Model memilih abstain; tidak ada tier risiko definitif.",
                audit, payload, List.copyOf(warnings));
    }

    private RiskPredictionRequest buildRequest(UUID sessionId, String requestId) {
        Profile profile = profileRepository.findById(sessionId).orElseThrow();
        List<MealEntry> meals = mealService.findAll(sessionId);
        Map<UUID, List<MealEntry>> mealsByObservation = meals.stream()
                .collect(Collectors.groupingBy(meal -> meal.getDailyObservation().getId()));

        List<RiskDayRequest> days = observationService.findAll(sessionId).stream()
                .map(day -> riskDay(day, mealsByObservation.getOrDefault(day.getId(), List.of())))
                .toList();

        RiskProfileRequest riskProfile = new RiskProfileRequest(
                profile.getAgeYears(), profile.getSexAtBirth(), profile.getHeightCm(), profile.getWeightKg(),
                profile.getWaistCircumferenceCm(), profile.getFamilyHistoryDiabetes(),
                profile.getHypertension(), false, false, false
        );
        return new RiskPredictionRequest(riskProfile, days, 14, requestId);
    }

    private RiskDayRequest riskDay(DailyObservation day, List<MealEntry> meals) {
        List<RiskMealRequest> mappedMeals = meals.stream().map(meal -> new RiskMealRequest(
                meal.getMealTime(), meal.getCaloriesKcal(), meal.getCarbohydrateG(), meal.getProteinG(),
                meal.getFatG(), meal.getSugarG(), meal.getFiberG(),
                new RiskProvenanceRequest(meal.getSource(), meal.getSourceVersion(), meal.isConfirmedByUser())
        )).toList();
        return new RiskDayRequest(
                day.getObservedOn(), observationService.readMims(day), day.getWearHours(), day.getSteps(), mappedMeals
        );
    }

    private ScreeningOutcome failureOutcome(ScreeningAudit audit, RiskClientStatus status) {
        HttpStatus httpStatus;
        ApplicationCode code;
        String applicationStatus;
        String message;
        switch (status) {
            case UNAUTHORIZED -> {
                httpStatus = HttpStatus.UNAUTHORIZED;
                code = ApplicationCode.UNAUTHORIZED;
                applicationStatus = "UNAUTHORIZED";
                message = "Konfigurasi akses model ditolak.";
            }
            case VALIDATION_ERROR -> {
                httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
                code = ApplicationCode.VALIDATION_ERROR;
                applicationStatus = "VALIDATION_ERROR";
                message = "Model menolak request yang telah divalidasi backend.";
            }
            case TIMEOUT -> {
                httpStatus = HttpStatus.GATEWAY_TIMEOUT;
                code = ApplicationCode.SERVICE_TIMEOUT;
                applicationStatus = "SERVICE_TIMEOUT";
                message = "Layanan model melewati batas waktu; screening tidak diulang otomatis.";
            }
            case UNAVAILABLE -> {
                httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
                code = ApplicationCode.MODEL_UNAVAILABLE;
                applicationStatus = "MODEL_UNAVAILABLE";
                message = "Model risiko belum tersedia; tidak ada hasil screening yang dibuat.";
            }
            default -> {
                httpStatus = HttpStatus.BAD_GATEWAY;
                code = ApplicationCode.NETWORK_ERROR;
                applicationStatus = "NETWORK_ERROR";
                message = "Respons model tidak dapat digunakan; tidak ada hasil screening yang dibuat.";
            }
        }
        String modelStatus = status == RiskClientStatus.UNAVAILABLE ? "not_ready" : null;
        audit.complete(applicationStatus, modelStatus, "{}", clock.instant());
        auditRepository.save(audit);
        return outcome(httpStatus, code, message, audit, null,
                List.of("Silakan lakukan retry secara sadar setelah memeriksa status layanan."));
    }

    private ScreeningOutcome outcome(
            HttpStatus status,
            ApplicationCode code,
            String message,
            ScreeningAudit audit,
            RiskPredictionPayload payload,
            List<String> warnings
    ) {
        return new ScreeningOutcome(
                status,
                code,
                message,
                new ScreeningResponse(
                        audit.getId(), audit.getApplicationStatus(), audit.getRequestedAt(), audit.getCompletedAt(),
                        payload == null ? null : ScreeningResultResponse.from(payload)
                ),
                warnings
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not encode screening provenance", exception);
        }
    }
}
