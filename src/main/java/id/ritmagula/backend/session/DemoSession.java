package id.ritmagula.backend.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "demo_session", schema = "ritmagula_app")
public class DemoSession {

    @Id
    private UUID id;

    @Column(name = "fixture_code", length = 50)
    private String fixtureCode;

    @Column(name = "data_classification", nullable = false, length = 32)
    private String dataClassification;

    @Column(name = "lifecycle_state", nullable = false, length = 16)
    private String lifecycleState;

    @Column(name = "observation_start_date", nullable = false)
    private LocalDate observationStartDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consented_at", nullable = false)
    private Instant consentedAt;

    @Column(name = "reset_at")
    private Instant resetAt;

    protected DemoSession() {
    }

    DemoSession(UUID id, String fixtureCode, LocalDate observationStartDate, Instant now) {
        this.id = id;
        this.fixtureCode = fixtureCode;
        this.dataClassification = "FICTIONAL_DEMO";
        this.lifecycleState = "ACTIVE";
        this.observationStartDate = observationStartDate;
        this.createdAt = now;
        this.expiresAt = now.plusSeconds(24 * 60 * 60);
        this.consentedAt = now;
    }

    public UUID getId() { return id; }
    public String getFixtureCode() { return fixtureCode; }
    public String getDataClassification() { return dataClassification; }
    public String getLifecycleState() { return lifecycleState; }
    public LocalDate getObservationStartDate() { return observationStartDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsentedAt() { return consentedAt; }
    public Instant getResetAt() { return resetAt; }

    public boolean isActiveAt(Instant now) {
        return "ACTIVE".equals(lifecycleState) && resetAt == null && expiresAt.isAfter(now);
    }
}
