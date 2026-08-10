package id.ritmagula.backend.observation;

import id.ritmagula.backend.session.DemoSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "daily_observation", schema = "ritmagula_app")
public class DailyObservation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DemoSession session;

    @Column(name = "observed_on", nullable = false)
    private LocalDate observedOn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hourly_mims", nullable = false, columnDefinition = "jsonb")
    private String hourlyMimsJson;

    @Column(name = "wear_hours", nullable = false, precision = 4, scale = 2)
    private BigDecimal wearHours;

    private Integer steps;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyObservation() {
    }

    public DailyObservation(DemoSession session, LocalDate observedOn, Instant now) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.observedOn = observedOn;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String hourlyMimsJson, BigDecimal wearHours, Integer steps, Instant now) {
        this.hourlyMimsJson = hourlyMimsJson;
        this.wearHours = wearHours;
        this.steps = steps;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public DemoSession getSession() { return session; }
    public LocalDate getObservedOn() { return observedOn; }
    public String getHourlyMimsJson() { return hourlyMimsJson; }
    public BigDecimal getWearHours() { return wearHours; }
    public Integer getSteps() { return steps; }
    public Instant getUpdatedAt() { return updatedAt; }
}
