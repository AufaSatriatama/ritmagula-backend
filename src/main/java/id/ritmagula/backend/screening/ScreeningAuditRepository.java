package id.ritmagula.backend.screening;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningAuditRepository extends JpaRepository<ScreeningAudit, UUID> {

    void deleteBySession_Id(UUID sessionId);
}
