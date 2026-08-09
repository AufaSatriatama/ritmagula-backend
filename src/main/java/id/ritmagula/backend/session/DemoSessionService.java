package id.ritmagula.backend.session;

import id.ritmagula.backend.api.ApiException;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.meal.MealEntryRepository;
import id.ritmagula.backend.observation.DailyObservationRepository;
import id.ritmagula.backend.profile.ProfileRepository;
import id.ritmagula.backend.screening.ScreeningAuditRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoSessionService {

    private final DemoSessionRepository repository;
    private final ScreeningAuditRepository screeningAuditRepository;
    private final MealEntryRepository mealEntryRepository;
    private final DailyObservationRepository observationRepository;
    private final ProfileRepository profileRepository;
    private final Clock clock = Clock.systemUTC();

    public DemoSessionService(
            DemoSessionRepository repository,
            ScreeningAuditRepository screeningAuditRepository,
            MealEntryRepository mealEntryRepository,
            DailyObservationRepository observationRepository,
            ProfileRepository profileRepository
    ) {
        this.repository = repository;
        this.screeningAuditRepository = screeningAuditRepository;
        this.mealEntryRepository = mealEntryRepository;
        this.observationRepository = observationRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public DemoSessionResponse create(CreateDemoSessionRequest request) {
        Instant now = clock.instant();
        DemoSession session = new DemoSession(
                UUID.randomUUID(),
                request.fixtureCode(),
                request.observationStartDate(),
                now
        );
        return DemoSessionResponse.from(repository.save(session));
    }

    @Transactional(readOnly = true)
    public DemoSession requireActive(UUID sessionId) {
        DemoSession session = repository.findById(sessionId)
                .orElseThrow(this::unauthorized);
        if (!session.isActiveAt(clock.instant())) {
            throw unauthorized();
        }
        return session;
    }

    @Transactional
    public void delete(UUID sessionId) {
        DemoSession session = requireActive(sessionId);
        screeningAuditRepository.deleteBySession_Id(sessionId);
        mealEntryRepository.deleteByDailyObservation_Session_Id(sessionId);
        observationRepository.deleteBySession_Id(sessionId);
        profileRepository.deleteById(sessionId);
        repository.delete(session);
    }

    private ApiException unauthorized() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                ApplicationCode.UNAUTHORIZED,
                "Sesi demo tidak ditemukan atau sudah kedaluwarsa."
        );
    }
}
