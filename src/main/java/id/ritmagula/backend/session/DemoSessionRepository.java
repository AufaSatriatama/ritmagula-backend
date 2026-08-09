package id.ritmagula.backend.session;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoSessionRepository extends JpaRepository<DemoSession, UUID> {
}
