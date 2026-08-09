package id.ritmagula.backend.model.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import id.ritmagula.backend.api.RequestIdFilter;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpFoodModelClientTests {

    @Test
    void preservesExperimentalAnalyzeResponseWithNutritionNull() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://food.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpFoodModelClient client = new HttpFoodModelClient(builder.build());
        server.expect(requestTo("http://food.test/v2/food/analyze"))
                .andExpect(header(RequestIdFilter.HEADER_NAME, "food-request-1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("""
                        {"request_id":"analysis-12345","status":"partial","clinical_use_allowed":false,
                         "quality":{"passed":true},"candidates":[{"label":"nasi_goreng","probability":0.7}],
                         "nutrition":null,"requires_user_confirmation":true,
                         "model_versions":{"recognizer":"1.0"},"abstention_reasons":[],"warnings":[],
                         "usage_mode":"mvp_assist","evidence_grade":"experimental_assist",
                         "portion_suggestions":[],"confirmation_questions":[]}
                        """));

        FoodAnalysisResult result = client.analyze(
                "food-request-1", new byte[]{1, 2, 3}, "food-upload.jpg", "image/jpeg", BigDecimal.valueOf(25)
        );

        assertThat(result.status()).isEqualTo(FoodClientStatus.SUCCESS);
        assertThat(result.payload().status()).isEqualTo("partial");
        assertThat(result.payload().nutrition().isNull()).isTrue();
        assertThat(result.payload().requiresUserConfirmation()).isTrue();
        server.verify();
    }

    @Test
    void rejectsConfirmationThatIsNotUserConfirmedFoodCv() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://food.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpFoodModelClient client = new HttpFoodModelClient(builder.build());
        server.expect(requestTo("http://food.test/v2/food/confirm"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("""
                        {"request_id":"confirm-12345","analysis_request_id":"analysis-12345",
                         "status":"confirmed","clinical_use_allowed":false,"selected_label":"nasi_goreng",
                         "display_name":"Nasi goreng","portion_mass":{"estimate":300},
                         "nutrition":{"calories":{"estimate":510}},"basis":{"catalog_version":"tkpi-v1"},
                         "journal_meal":{"time":"12:00:00","calories_kcal":510,"carbohydrate_g":62,
                         "protein_g":18,"fat_g":20,"provenance":{"source":"food_cv",
                         "confirmed_by_user":false,"source_version":"tkpi-v1"}},
                         "model_versions":{"recognizer":"1.0"},"warnings":[]}
                        """));

        FoodConfirmationResult result = client.confirm("food-request-2", command());

        assertThat(result.status()).isEqualTo(FoodClientStatus.INVALID_RESPONSE);
        assertThat(result.payload()).isNull();
        server.verify();
    }

    @Test
    void mapsHttp503ToUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://food.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpFoodModelClient client = new HttpFoodModelClient(builder.build());
        server.expect(requestTo("http://food.test/v2/food/confirm"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(client.confirm("food-request-3", command()).status())
                .isEqualTo(FoodClientStatus.UNAVAILABLE);
        server.verify();
    }

    private FoodConfirmationCommand command() {
        return new FoodConfirmationCommand(
                "analysis-12345", "nasi_goreng", "medium", null,
                BigDecimal.ONE, BigDecimal.ONE,
                new FoodModifierCommand(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                LocalTime.NOON, true
        );
    }
}
