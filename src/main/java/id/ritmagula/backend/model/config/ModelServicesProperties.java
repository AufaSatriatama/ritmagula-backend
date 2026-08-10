package id.ritmagula.backend.model.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ritmagula.models")
public record ModelServicesProperties(
        @Valid @NotNull Service risk,
        @Valid @NotNull Service food,
        String apiKey
) {
    public record Service(
            @NotNull URI baseUrl,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout
    ) {
    }
}
