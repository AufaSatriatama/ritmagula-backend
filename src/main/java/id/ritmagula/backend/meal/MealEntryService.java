package id.ritmagula.backend.meal;

import id.ritmagula.backend.api.ApiException;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.observation.ActivityRequest;
import id.ritmagula.backend.observation.DailyObservation;
import id.ritmagula.backend.observation.DailyObservationRepository;
import id.ritmagula.backend.observation.DailyObservationService;
import id.ritmagula.backend.session.DemoSession;
import id.ritmagula.backend.session.DemoSessionService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealEntryService {

    private final MealEntryRepository repository;
    private final DailyObservationRepository observationRepository;
    private final DailyObservationService observationService;
    private final DemoSessionService sessionService;
    private final Clock clock = Clock.systemUTC();

    public MealEntryService(
            MealEntryRepository repository,
            DailyObservationRepository observationRepository,
            DailyObservationService observationService,
            DemoSessionService sessionService
    ) {
        this.repository = repository;
        this.observationRepository = observationRepository;
        this.observationService = observationService;
        this.sessionService = sessionService;
    }

    @Transactional
    public MealEntryResponse add(UUID sessionId, LocalDate date, MealRequest request) {
        DemoSession session = sessionService.requireActive(sessionId);
        ensureDate(session, date);
        DailyObservation observation = observationRepository.findBySession_IdAndObservedOn(sessionId, date)
                .orElseGet(() -> createEmptyObservation(sessionId, date));
        if (repository.countByDailyObservation_Id(observation.getId()) >= 20) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Maksimal 20 makanan dapat disimpan per hari."
            );
        }
        return MealEntryResponse.from(repository.save(new MealEntry(observation, request, clock.instant())));
    }

    @Transactional
    public MealEntryResponse addConfirmedFood(UUID sessionId, LocalDate date, ConfirmedFoodMeal meal) {
        DemoSession session = sessionService.requireActive(sessionId);
        ensureDate(session, date);
        if (repository.existsByAnalysisRequestId(meal.analysisRequestId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ApplicationCode.VALIDATION_ERROR,
                    "Konfirmasi Food AI ini sudah pernah masuk jurnal."
            );
        }
        DailyObservation observation = observationRepository.findBySession_IdAndObservedOn(sessionId, date)
                .orElseGet(() -> createEmptyObservation(sessionId, date));
        if (repository.countByDailyObservation_Id(observation.getId()) >= 20) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Maksimal 20 makanan dapat disimpan per hari."
            );
        }
        return MealEntryResponse.from(repository.save(new MealEntry(observation, meal, clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<MealEntry> findAll(UUID sessionId) {
        sessionService.requireActive(sessionId);
        return repository.findByDailyObservation_Session_IdOrderByDailyObservation_ObservedOnAscMealTimeAsc(sessionId);
    }

    private DailyObservation createEmptyObservation(UUID sessionId, LocalDate date) {
        ActivityRequest empty = new ActivityRequest(
                Collections.nCopies(24, null), BigDecimal.ZERO, null
        );
        observationService.save(sessionId, date, empty);
        return observationRepository.findBySession_IdAndObservedOn(sessionId, date).orElseThrow();
    }

    private void ensureDate(DemoSession session, LocalDate date) {
        if (date.isBefore(session.getObservationStartDate())
                || date.isAfter(session.getObservationStartDate().plusDays(13))) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Tanggal makanan harus berada dalam interval observasi 14 hari."
            );
        }
    }
}
