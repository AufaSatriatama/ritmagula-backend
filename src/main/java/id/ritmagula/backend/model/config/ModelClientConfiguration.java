package id.ritmagula.backend.model.config;

import id.ritmagula.backend.model.health.HttpModelHealthClient;
import id.ritmagula.backend.model.health.ModelHealthClient;
import id.ritmagula.backend.model.risk.HttpRiskPredictionClient;
import id.ritmagula.backend.model.risk.RiskPredictionClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class ModelClientConfiguration {

    @Bean
    @Qualifier("riskModelHealthClient")
    ModelHealthClient riskModelHealthClient(ModelServicesProperties properties) {
        return createHealthClient("risk", properties.risk(), properties.apiKey());
    }

    @Bean
    @Qualifier("foodModelHealthClient")
    ModelHealthClient foodModelHealthClient(ModelServicesProperties properties) {
        return createHealthClient("food", properties.food(), properties.apiKey());
    }

    @Bean
    RiskPredictionClient riskPredictionClient(ModelServicesProperties properties) {
        return new HttpRiskPredictionClient(createRestClient(properties.risk(), properties.apiKey()));
    }

    private ModelHealthClient createHealthClient(
            String serviceName,
            ModelServicesProperties.Service service,
            String apiKey
    ) {
        return new HttpModelHealthClient(serviceName, createRestClient(service, apiKey));
    }

    private RestClient createRestClient(ModelServicesProperties.Service service, String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(service.connectTimeout());
        requestFactory.setReadTimeout(service.readTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(service.baseUrl().toString())
                .requestFactory(requestFactory);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }

        return builder.build();
    }
}
