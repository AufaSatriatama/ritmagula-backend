package id.ritmagula.backend.meal;

import id.ritmagula.backend.observation.DailyObservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "meal_entry", schema = "ritmagula_app")
public class MealEntry {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_observation_id", nullable = false)
    private DailyObservation dailyObservation;
    @Column(name = "meal_time", nullable = false)
    private LocalTime mealTime;
    @Column(name = "calories_kcal", nullable = false, precision = 8, scale = 3)
    private BigDecimal caloriesKcal;
    @Column(name = "carbohydrate_g", nullable = false, precision = 8, scale = 3)
    private BigDecimal carbohydrateG;
    @Column(name = "protein_g", nullable = false, precision = 8, scale = 3)
    private BigDecimal proteinG;
    @Column(name = "fat_g", nullable = false, precision = 8, scale = 3)
    private BigDecimal fatG;
    @Column(name = "sugar_g", precision = 8, scale = 3)
    private BigDecimal sugarG;
    @Column(name = "fiber_g", precision = 8, scale = 3)
    private BigDecimal fiberG;
    @Column(nullable = false, length = 16)
    private String source;
    @Column(name = "source_version", length = 100)
    private String sourceVersion;
    @Column(name = "confirmed_by_user", nullable = false)
    private boolean confirmedByUser;
    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MealEntry() {
    }

    public MealEntry(DailyObservation observation, MealRequest request, Instant now) {
        this.id = UUID.randomUUID();
        this.dailyObservation = observation;
        this.mealTime = request.time();
        this.caloriesKcal = request.caloriesKcal();
        this.carbohydrateG = request.carbohydrateG();
        this.proteinG = request.proteinG();
        this.fatG = request.fatG();
        this.sugarG = request.sugarG();
        this.fiberG = request.fiberG();
        this.source = "manual";
        this.sourceVersion = request.sourceVersion();
        this.confirmedByUser = true;
        this.confirmedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public DailyObservation getDailyObservation() { return dailyObservation; }
    public LocalTime getMealTime() { return mealTime; }
    public BigDecimal getCaloriesKcal() { return caloriesKcal; }
    public BigDecimal getCarbohydrateG() { return carbohydrateG; }
    public BigDecimal getProteinG() { return proteinG; }
    public BigDecimal getFatG() { return fatG; }
    public BigDecimal getSugarG() { return sugarG; }
    public BigDecimal getFiberG() { return fiberG; }
    public String getSource() { return source; }
    public String getSourceVersion() { return sourceVersion; }
    public boolean isConfirmedByUser() { return confirmedByUser; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
