package id.ritmagula.backend.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile", schema = "ritmagula_app")
public class Profile {

    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "age_years", nullable = false)
    private short ageYears;
    @Column(name = "sex_at_birth", nullable = false, length = 8)
    private String sexAtBirth;
    @Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal heightCm;
    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;
    @Column(name = "family_history_diabetes")
    private Boolean familyHistoryDiabetes;
    @Column(name = "hypertension")
    private Boolean hypertension;
    @Column(nullable = false)
    private boolean pregnant;
    @Column(name = "diagnosed_diabetes", nullable = false)
    private boolean diagnosedDiabetes;
    @Column(name = "taking_diabetes_medication", nullable = false)
    private boolean takingDiabetesMedication;
    @Column(name = "eligibility_confirmed_at", nullable = false)
    private Instant eligibilityConfirmedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Profile() {
    }

    public Profile(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public void update(ProfileRequest request, Instant now) {
        this.ageYears = request.ageYears().shortValue();
        this.sexAtBirth = request.sexAtBirth();
        this.heightCm = request.heightCm();
        this.weightKg = request.weightKg();
        this.familyHistoryDiabetes = request.familyHistoryDiabetes();
        this.hypertension = request.hypertension();
        this.pregnant = request.pregnant();
        this.diagnosedDiabetes = request.diagnosedDiabetes();
        this.takingDiabetesMedication = request.takingDiabetesMedication();
        this.eligibilityConfirmedAt = now;
        this.updatedAt = now;
    }

    public UUID getSessionId() { return sessionId; }
    public int getAgeYears() { return ageYears; }
    public String getSexAtBirth() { return sexAtBirth; }
    public BigDecimal getHeightCm() { return heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public Boolean getFamilyHistoryDiabetes() { return familyHistoryDiabetes; }
    public Boolean getHypertension() { return hypertension; }
    public boolean isPregnant() { return pregnant; }
    public boolean isDiagnosedDiabetes() { return diagnosedDiabetes; }
    public boolean isTakingDiabetesMedication() { return takingDiabetesMedication; }
    public Instant getEligibilityConfirmedAt() { return eligibilityConfirmedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
