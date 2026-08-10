package id.ritmagula.backend.model.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import id.ritmagula.backend.api.RequestIdFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpRiskPredictionClientTests {

    @Test
    void preservesAbstainedResponseWithoutInventingResult() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://risk.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpRiskPredictionClient client = new HttpRiskPredictionClient(builder.build());

        server.expect(requestTo("http://risk.test/v2/risk/predict"))
                .andExpect(header(RequestIdFilter.HEADER_NAME, "request-123"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("""
                        {"request_id":"model-1","status":"abstained","class_probabilities":null,
                         "dysglycemia_probability":null,"risk_level":"tidak_pasti",
                         "conformal_prediction_set":[],"modality_quality":[],"driving_factors":[],
                         "uncertainty":{},"model_versions":{"risk":"2.0.0-research"},
                         "recommendation":"Data belum cukup.","warnings":[],
                         "abstention_reasons":["quality"],"clinical_use_allowed":false}
                        """));

        RiskPredictionResult result = client.predict("request-123", emptyRequest());

        assertThat(result.status()).isEqualTo(RiskClientStatus.SUCCESS);
        assertThat(result.payload().status()).isEqualTo("abstained");
        assertThat(result.payload().classProbabilities()).isEmpty();
        assertThat(result.payload().abstentionReasons()).containsExactly("quality");
        server.verify();
    }

    @Test
    void mapsHttp503ToUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://risk.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpRiskPredictionClient client = new HttpRiskPredictionClient(builder.build());
        server.expect(requestTo("http://risk.test/v2/risk/predict"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(client.predict("request-123", emptyRequest()).status())
                .isEqualTo(RiskClientStatus.UNAVAILABLE);
        server.verify();
    }

    @Test
    void rejectsOkResponseWithIncompleteProbabilityContract() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://risk.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpRiskPredictionClient client = new HttpRiskPredictionClient(builder.build());
        server.expect(requestTo("http://risk.test/v2/risk/predict"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("""
                        {"request_id":"model-2","status":"ok",
                         "class_probabilities":{"normal":0.6,"prediabetes":0.4},
                         "dysglycemia_probability":0.4,"risk_level":"meningkat",
                         "conformal_prediction_set":["normal","prediabetes"],"modality_quality":[],
                         "driving_factors":[],"uncertainty":{},"model_versions":{"risk":"2.1.0"},
                         "recommendation":"Screening riset.","warnings":[],"abstention_reasons":[],
                         "clinical_use_allowed":false}
                        """));

        RiskPredictionResult result = client.predict("request-456", emptyRequest());

        assertThat(result.status()).isEqualTo(RiskClientStatus.INVALID_RESPONSE);
        assertThat(result.payload()).isNull();
        server.verify();
    }

    private RiskPredictionRequest emptyRequest() {
        return new RiskPredictionRequest(null, List.of(), 14, "test");
    }
}
