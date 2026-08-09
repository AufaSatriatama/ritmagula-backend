package id.ritmagula.backend;

import id.ritmagula.backend.model.config.ModelServicesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ModelServicesProperties.class)
public class RitmagulaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RitmagulaBackendApplication.class, args);
    }
}
