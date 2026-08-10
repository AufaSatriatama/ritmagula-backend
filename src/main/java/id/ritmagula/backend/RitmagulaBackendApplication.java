package id.ritmagula.backend;

import id.ritmagula.backend.model.config.ModelServicesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(ModelServicesProperties.class)
@EnableScheduling
public class RitmagulaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RitmagulaBackendApplication.class, args);
    }
}
