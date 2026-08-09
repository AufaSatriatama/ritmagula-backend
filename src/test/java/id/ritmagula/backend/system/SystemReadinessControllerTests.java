package id.ritmagula.backend.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ritmagula.backend.api.RequestIdFilter;
import id.ritmagula.backend.model.health.ModelServiceReadiness;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SystemReadinessControllerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ModelServiceReadiness risk = new ModelServiceReadiness(
                "risk", true, false, "not_ready", "0.2.0", false, Map.of(), "missing artifact"
        );
        ModelServiceReadiness food = new ModelServiceReadiness(
                "food", true, false, "not_ready", "0.2.0", false, Map.of(), "not promoted"
        );
        SystemReadinessService service = new SystemReadinessService(
                requestId -> risk,
                requestId -> food
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SystemReadinessController(service))
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void reportsApplicationAndModelReadinessWithoutInventingAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/system/readiness")
                        .header(RequestIdFilter.HEADER_NAME, "frontend-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "frontend-request-1"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.requestId").value("frontend-request-1"))
                .andExpect(jsonPath("$.code").value("SYSTEM_READY"))
                .andExpect(jsonPath("$.data.applicationReady").value(true))
                .andExpect(jsonPath("$.data.screeningAvailable").value(false))
                .andExpect(jsonPath("$.data.foodAnalysisAvailable").value(false))
                .andExpect(jsonPath("$.data.risk.status").value("not_ready"))
                .andExpect(jsonPath("$.warnings.length()").value(2));
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/system/readiness")
                        .header(RequestIdFilter.HEADER_NAME, "invalid request id"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME,
                        org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
    }
}
