package id.ritmagula.backend.screening;

import id.ritmagula.backend.session.DemoSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "screening_audit", schema = "ritmagula_app")
public class ScreeningAudit {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DemoSession session;
    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;
    @Column(name = "application_status", nullable = false, length = 32)
    private String applicationStatus;
    @Column(name = "model_status", length = 16)
    private String modelStatus;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_versions", nullable = false, columnDefinition = "jsonb")
    private String modelVersionsJson;

    protected ScreeningAudit() {
    }

    public ScreeningAudit(DemoSession session, String requestId, Instant now) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.requestId = requestId;
        this.applicationStatus = "SCREENING_REQUESTED";
        this.requestedAt = now;
        this.modelVersionsJson = "{}";
    }

    public void complete(String applicationStatus, String modelStatus, String modelVersionsJson, Instant now) {
        this.applicationStatus = applicationStatus;
        this.modelStatus = modelStatus;
        this.modelVersionsJson = modelVersionsJson;
        this.completedAt = now;
    }

    public UUID getId() { return id; }
    public String getApplicationStatus() { return applicationStatus; }
    public String getModelStatus() { return modelStatus; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
