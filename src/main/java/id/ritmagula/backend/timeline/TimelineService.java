package id.ritmagula.backend.timeline;

import id.ritmagula.backend.meal.MealEntryService;
import id.ritmagula.backend.observation.DailyObservation;
import id.ritmagula.backend.observation.DailyObservationService;
import id.ritmagula.backend.profile.ProfileRepository;
import id.ritmagula.backend.session.DemoSession;
import id.ritmagula.backend.session.DemoSessionService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimelineService {

    private final DemoSessionService sessionService;
    private final ProfileRepository profileRepository;
    private final DailyObservationService observationService;
    private final MealEntryService mealService;

    public TimelineService(
            DemoSessionService sessionService,
            ProfileRepository profileRepository,
            DailyObservationService observationService,
            MealEntryService mealService
    ) {
        this.sessionService = sessionService;
        this.profileRepository = profileRepository;
        this.observationService = observationService;
        this.mealService = mealService;
    }

    @Transactional(readOnly = true)
    public TimelineResponse build(UUID sessionId) {
        DemoSession session = sessionService.requireActive(sessionId);
        Map<LocalDate, DailyObservation> observations = observationService.findAll(sessionId).stream()
                .collect(Collectors.toMap(DailyObservation::getObservedOn, Function.identity()));
        Map<LocalDate, Long> mealsPerDay = mealService.findAll(sessionId).stream()
                .collect(Collectors.groupingBy(
                        meal -> meal.getDailyObservation().getObservedOn(),
                        Collectors.counting()
                ));

        List<DayStatus> days = new ArrayList<>();
        for (int offset = 0; offset < 14; offset++) {
            LocalDate date = session.getObservationStartDate().plusDays(offset);
            DailyObservation observation = observations.get(date);
            days.add(new DayStatus(
                    date,
                    observation != null,
                    observation != null && observationService.validActivity(observation),
                    mealsPerDay.getOrDefault(date, 0L).intValue()
            ));
        }

        WindowStatus first = window(1, days.subList(0, 7));
        WindowStatus second = window(2, days.subList(7, 14));
        boolean profileComplete = profileRepository.existsById(sessionId);
        boolean ready = profileComplete && first.usable() && second.usable();

        List<String> actions = new ArrayList<>();
        if (!profileComplete) {
            actions.add("Lengkapi profil dan konfirmasi kelayakan.");
        }
        addWindowAction(actions, first);
        addWindowAction(actions, second);

        return new TimelineResponse(
                sessionId,
                session.getObservationStartDate(),
                session.getObservationStartDate().plusDays(13),
                profileComplete,
                List.copyOf(days),
                List.of(first, second),
                ready,
                List.copyOf(actions)
        );
    }

    private WindowStatus window(int number, List<DayStatus> days) {
        int activityDays = (int) days.stream().filter(DayStatus::validActivity).count();
        int mealDays = (int) days.stream().filter(day -> day.confirmedMeals() > 0).count();
        return new WindowStatus(
                number,
                days.getFirst().date(),
                days.getLast().date(),
                activityDays,
                mealDays,
                activityDays >= 4 && mealDays >= 3
        );
    }

    private void addWindowAction(List<String> actions, WindowStatus window) {
        if (window.validActivityDays() < 4) {
            actions.add("Window " + window.window() + " masih membutuhkan "
                    + (4 - window.validActivityDays()) + " hari aktivitas valid.");
        }
        if (window.confirmedMealDays() < 3) {
            actions.add("Window " + window.window() + " masih membutuhkan "
                    + (3 - window.confirmedMealDays()) + " hari makanan terkonfirmasi.");
        }
    }
}
