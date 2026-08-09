package id.ritmagula.backend.observation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyObservationRepository extends JpaRepository<DailyObservation, UUID> {

    Optional<DailyObservation> findBySession_IdAndObservedOn(UUID sessionId, LocalDate observedOn);

    List<DailyObservation> findBySession_IdOrderByObservedOnAsc(UUID sessionId);

    void deleteBySession_Id(UUID sessionId);
}
