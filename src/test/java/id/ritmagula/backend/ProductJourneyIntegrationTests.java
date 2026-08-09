package id.ritmagula.backend;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ritmagula.backend.model.health.ModelHealthClient;
import id.ritmagula.backend.model.health.ModelServiceReadiness;
import id.ritmagula.backend.model.food.ConfirmedMealPayload;
import id.ritmagula.backend.model.food.FoodAnalysisResult;
import id.ritmagula.backend.model.food.FoodAnalyzePayload;
import id.ritmagula.backend.model.food.FoodClientStatus;
import id.ritmagula.backend.model.food.FoodConfirmationResult;
import id.ritmagula.backend.model.food.FoodConfirmPayload;
import id.ritmagula.backend.model.food.FoodModelClient;
import id.ritmagula.backend.model.food.FoodProvenancePayload;
import id.ritmagula.backend.model.risk.RiskPredictionClient;
import id.ritmagula.backend.model.risk.RiskPredictionPayload;
import id.ritmagula.backend.model.risk.RiskPredictionResult;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
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

    @MockitoBean(name = "foodModelHealthClient")
    private ModelHealthClient foodHealthClient;

    @MockitoBean
    private FoodModelClient foodModelClient;

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

    @Test
    void foodAnalysisFailsSafelyWithoutCallingModelWhenServiceIsNotReady() throws Exception {
        UUID sessionId = createSession();
        when(foodHealthClient.check(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                new ModelServiceReadiness(
                        "food", true, false, "not_ready", "0.2.0", false, Map.of(), "missing artifacts"
                )
        );
        MockMultipartFile image = new MockMultipartFile(
                "image", "personal-name.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/demo-sessions/{id}/food-analyses", sessionId)
                        .file(image)
                        .param("plateDiameterCm", "25")
                        .header("X-Request-ID", "food-unavailable-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("MODEL_UNAVAILABLE"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verifyNoInteractions(foodModelClient);
    }

    @Test
    void foodRequiresCorrectionThenPersistsOnlyConfirmedTkpiMealWithProvenance() throws Exception {
        UUID sessionId = createSession();
        when(foodHealthClient.check(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                new ModelServiceReadiness(
                        "food", true, true, "ready", "0.2.0", false,
                        Map.of("segmenter", "1.0", "recognizer", "1.0"), null
                )
        );
        FoodAnalyzePayload analysis = new FoodAnalyzePayload(
                "analysis-12345", "partial", false, objectMapper.readTree("{\"passed\":true}"),
                null, null, List.of(objectMapper.readTree("{\"label\":\"nasi_goreng\",\"probability\":0.7}")),
                false, null, true, Map.of("recognizer", "1.0"), List.of(), List.of(),
                "mvp_assist", "experimental_assist", List.of(), List.of(), "tkpi-2026-v1"
        );
        when(foodModelClient.analyze(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new FoodAnalysisResult(FoodClientStatus.SUCCESS, analysis));

        MockMultipartFile image = new MockMultipartFile(
                "image", "personal-name.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        mockMvc.perform(multipart("/api/v1/demo-sessions/{id}/food-analyses", sessionId)
                        .file(image)
                        .param("plateDiameterCm", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PARTIAL_CONFIRMATION_REQUIRED"))
                .andExpect(jsonPath("$.data.request_id").value("analysis-12345"))
                .andExpect(jsonPath("$.data.nutrition").doesNotExist())
                .andExpect(jsonPath("$.data.requires_user_confirmation").value(true));

        ConfirmedMealPayload journal = new ConfirmedMealPayload(
                LocalTime.NOON, new BigDecimal("510"), new BigDecimal("62"),
                new BigDecimal("18"), new BigDecimal("20"), null, new BigDecimal("4"),
                new FoodProvenancePayload("food_cv", true, "tkpi-2026-v1")
        );
        FoodConfirmPayload confirmation = new FoodConfirmPayload(
                "confirm-12345", "analysis-12345", "confirmed", false,
                "nasi_goreng", "Nasi goreng", objectMapper.readTree("{\"estimate\":300}"),
                objectMapper.readTree("{\"calories\":{\"estimate\":510}}"),
                objectMapper.readTree("{\"catalog_version\":\"tkpi-2026-v1\"}"), journal,
                Map.of("recognizer", "1.0", "nutrition_profile", "tkpi-2026-v1"), List.of()
        );
        when(foodModelClient.confirm(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(new FoodConfirmationResult(FoodClientStatus.SUCCESS, confirmation));

        mockMvc.perform(post("/api/v1/demo-sessions/{id}/days/{date}/food-confirmations", sessionId, START)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"analysisRequestId":"analysis-12345","selectedLabel":"nasi_goreng",
                                 "portionPreset":"medium","servings":1,"eatenFraction":1,
                                 "modifiers":{},"mealTime":"12:00:00","confirmedByUser":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmation.status").value("confirmed"))
                .andExpect(jsonPath("$.data.journalEntry.source").value("food_cv"))
                .andExpect(jsonPath("$.data.journalEntry.analysisRequestId").value("analysis-12345"))
                .andExpect(jsonPath("$.data.journalEntry.confirmedByUser").value(true));

        mockMvc.perform(get("/api/v1/demo-sessions/{id}/timeline", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].confirmedMeals").value(1));
    }

    @Test
    void forwardsWaistAndConfirmedFourteenDayInputsAndPreservesAbstention() throws Exception {
        UUID sessionId = createSession();
        saveProfile(sessionId);
        for (int offset : ACTIVITY_OFFSETS) {
            saveActivity(sessionId, START.plusDays(offset));
        }
        for (int offset : MEAL_OFFSETS) {
            saveMeal(sessionId, START.plusDays(offset));
        }

        when(riskHealthClient.check(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                new ModelServiceReadiness(
                        "risk", true, true, "ready", "0.2.0", false,
                        Map.of("risk", "2.1.0"), null
                )
        );
        RiskPredictionPayload abstained = new RiskPredictionPayload(
                "risk-result-1", "abstained", null, null, "tidak_pasti", List.of(),
                objectMapper.readTree("[]"), objectMapper.readTree("[]"), Map.of("reason", "quality"),
                Map.of("risk", "2.1.0"), "Lengkapi kualitas data.", List.of(),
                List.of("modality_quality"), false
        );
        when(riskPredictionClient.predict(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(new RiskPredictionResult(
                id.ritmagula.backend.model.risk.RiskClientStatus.SUCCESS, abstained
        ));

        mockMvc.perform(post("/api/v1/demo-sessions/{id}/screenings", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABSTAINED"))
                .andExpect(jsonPath("$.data.result.classProbabilities").isEmpty())
                .andExpect(jsonPath("$.data.result.abstentionReasons[0]").value("modality_quality"));

        ArgumentCaptor<id.ritmagula.backend.model.risk.RiskPredictionRequest> captor =
                ArgumentCaptor.forClass(id.ritmagula.backend.model.risk.RiskPredictionRequest.class);
        verify(riskPredictionClient).predict(org.mockito.ArgumentMatchers.anyString(), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().profile().waistCircumferenceCm())
                .isEqualByComparingTo("96");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().observationWindowDays()).isEqualTo(14);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().days()).hasSize(9);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().days().stream()
                .flatMap(day -> day.meals().stream())
                .allMatch(meal -> meal.provenance().confirmedByUser())).isTrue();
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
                 "waistCircumferenceCm":96,
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
