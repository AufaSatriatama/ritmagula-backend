package id.ritmagula.backend.observation;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import id.ritmagula.backend.api.ApiException;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.session.DemoSession;
import id.ritmagula.backend.session.DemoSessionService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyObservationService {

    private static final TypeReference<List<Double>> MIMS_TYPE = new TypeReference<>() { };

    private final DailyObservationRepository repository;
    private final DemoSessionService sessionService;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    public DailyObservationService(
            DailyObservationRepository repository,
            DemoSessionService sessionService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DailyObservationResponse save(UUID sessionId, LocalDate date, ActivityRequest request) {
        DemoSession session = sessionService.requireActive(sessionId);
        validateDate(session, date);
        validateMims(request.hourlyMims());

        DailyObservation observation = repository.findBySession_IdAndObservedOn(sessionId, date)
                .orElseGet(() -> new DailyObservation(session, date, clock.instant()));
        observation.update(writeMims(request.hourlyMims()), request.wearHours(), request.steps(), clock.instant());
        return response(repository.save(observation));
    }

    @Transactional(readOnly = true)
    public List<DailyObservation> findAll(UUID sessionId) {
        sessionService.requireActive(sessionId);
        return repository.findBySession_IdOrderByObservedOnAsc(sessionId);
    }

    public List<Double> readMims(DailyObservation observation) {
        try {
            return objectMapper.readValue(observation.getHourlyMimsJson(), MIMS_TYPE);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored hourly MIMS is invalid JSON", exception);
        }
    }

    public boolean validActivity(DailyObservation observation) {
        long presentHours = readMims(observation).stream().filter(value -> value != null).count();
        return observation.getWearHours().doubleValue() >= 10.0 && presentHours >= 18;
    }

    private DailyObservationResponse response(DailyObservation observation) {
        return new DailyObservationResponse(
                observation.getId(), observation.getSession().getId(), observation.getObservedOn(),
                readMims(observation), observation.getWearHours(), observation.getSteps(),
                validActivity(observation), observation.getUpdatedAt()
        );
    }

    private void validateDate(DemoSession session, LocalDate date) {
        LocalDate end = session.getObservationStartDate().plusDays(13);
        if (date.isBefore(session.getObservationStartDate()) || date.isAfter(end)) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Tanggal harus berada dalam interval observasi 14 hari."
            );
        }
    }

    private void validateMims(List<Double> values) {
        if (values.stream().filter(value -> value != null).anyMatch(value -> !Double.isFinite(value))) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Nilai MIMS harus berupa angka terbatas atau null."
            );
        }
    }

    private String writeMims(List<Double> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not encode hourly MIMS", exception);
        }
    }
}
