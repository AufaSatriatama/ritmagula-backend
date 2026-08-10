package id.ritmagula.backend.session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoSessionRepository extends JpaRepository<DemoSession, UUID> {

    List<DemoSession> findByExpiresAtLessThanEqualOrLifecycleState(Instant expiresAt, String lifecycleState);
}
