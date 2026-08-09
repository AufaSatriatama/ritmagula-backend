package id.ritmagula.backend;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ritmagula.backend.model.health.ModelHealthClient;
import id.ritmagula.backend.model.health.ModelServiceReadiness;
import id.ritmagula.backend.model.risk.RiskPredictionClient;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "debug=false",
        "spring.datasource.url=jdbc:h2:mem:journey;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS ritmagula_app",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class ProductJourneyIntegrationTests {

    private static final LocalDate START = LocalDate.of(2026, 7, 20);
    private static final int[] ACTIVITY_OFFSETS = {0, 1, 2, 3, 4, 7, 8, 9, 10};
    private static final int[] MEAL_OFFSETS = {0, 1, 2, 3, 7, 8, 9};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean(name = "riskModelHealthClient")
    private ModelHealthClient riskHealthClient;

    @MockitoBean
    private RiskPredictionClient riskPredictionClient;

    @Test
    void allowsOnlyConfiguredLocalFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/demo-sessions")
                        .header("Origin", "http://127.0.0.1:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,X-Request-ID"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:8081"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));

        mockMvc.perform(options("/api/v1/demo-sessions")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void completesP0CollectionAndFailsSafelyWhenRiskModelIsUnavailable() throws Exception {
        UUID sessionId = createSession();
        saveProfile(sessionId);

        for (int offset : ACTIVITY_OFFSETS) {
            saveActivity(sessionId, START.plusDays(offset));
        }
        for (int offset : MEAL_OFFSETS) {
            saveMeal(sessionId, START.plusDays(offset));
        }

        mockMvc.perform(get("/api/v1/demo-sessions/{id}/timeline", sessionId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("READY_TO_SCREEN"))
                .andExpect(jsonPath("$.data.readyToScreen").value(true))
                .andExpect(jsonPath("$.data.windows[0].validActivityDays").value(5))
                .andExpect(jsonPath("$.data.windows[0].confirmedMealDays").value(4))
                .andExpect(jsonPath("$.data.windows[1].validActivityDays").value(4))
                .andExpect(jsonPath("$.data.windows[1].confirmedMealDays").value(3));

        when(riskHealthClient.check(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                new ModelServiceReadiness(
                        "risk", true, false, "not_ready", "0.2.0", false, Map.of(), "missing artifact"
                )
        );

        mockMvc.perform(post("/api/v1/demo-sessions/{id}/screenings", sessionId)
                        .header("X-Request-ID", "journey-screening-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.requestId").value("journey-screening-1"))
                .andExpect(jsonPath("$.code").value("MODEL_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.status").value("MODEL_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.result").doesNotExist());

        mockMvc.perform(delete("/api/v1/demo-sessions/{id}", sessionId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/demo-sessions/{id}/timeline", sessionId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsOutsideScopeProfileWithoutPersistingIt() throws Exception {
        UUID sessionId = createSession();
        mockMvc.perform(put("/api/v1/demo-sessions/{id}/profile", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(true)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/demo-sessions/{id}/timeline", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileComplete").value(false));
    }

    private UUID createSession() throws Exception {
        String response = mockMvc.perform(post("/api/v1/demo-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentAccepted":true,"observationStartDate":"2026-07-20","fixtureCode":"RG-P0-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COLLECTING_DATA"))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
    }

    private void saveProfile(UUID sessionId) throws Exception {
        mockMvc.perform(put("/api/v1/demo-sessions/{id}/profile", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true));
    }

    private String profileJson(boolean diagnosed) {
        return """
                {"ageYears":38,"sexAtBirth":"male","heightCm":170,"weightKg":82,
                 "familyHistoryDiabetes":true,"hypertension":false,"pregnant":false,
                 "diagnosedDiabetes":%s,"takingDiabetesMedication":false}
                """.formatted(diagnosed);
    }

    private void saveActivity(UUID sessionId, LocalDate date) throws Exception {
        String values = String.join(",", java.util.Collections.nCopies(24, "0.25"));
        mockMvc.perform(put("/api/v1/demo-sessions/{id}/days/{date}/activity", sessionId, date)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"hourlyMims\":[" + values + "],\"wearHours\":12,\"steps\":7000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validActivity").value(true));
    }

    private void saveMeal(UUID sessionId, LocalDate date) throws Exception {
        mockMvc.perform(post("/api/v1/demo-sessions/{id}/days/{date}/meals", sessionId, date)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"time":"12:00:00","caloriesKcal":600,"carbohydrateG":75,
                                 "proteinG":25,"fatG":20,"confirmedByUser":true,
                                 "sourceVersion":"fixture-rg-p0-01-v1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedByUser").value(true))
                .andExpect(jsonPath("$.data.source").value("manual"));
    }
}
