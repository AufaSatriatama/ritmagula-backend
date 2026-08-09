package id.ritmagula.backend.meal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealEntryRepository extends JpaRepository<MealEntry, UUID> {

    long countByDailyObservation_Id(UUID observationId);

    boolean existsByAnalysisRequestId(String analysisRequestId);

    List<MealEntry> findByDailyObservation_Session_IdOrderByDailyObservation_ObservedOnAscMealTimeAsc(UUID sessionId);

    void deleteByDailyObservation_Session_Id(UUID sessionId);
}
