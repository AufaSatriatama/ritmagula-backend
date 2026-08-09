package id.ritmagula.backend.model.health;

import java.util.Map;

public record ModelServiceReadiness(
        String service,
        boolean reachable,
        boolean ready,
        String status,
        String serviceVersion,
        boolean clinicalUseAllowed,
        Map<String, String> modelVersions,
        String error
) {
    public ModelServiceReadiness {
        modelVersions = modelVersions == null ? Map.of() : Map.copyOf(modelVersions);
    }

    public static ModelServiceReadiness unreachable(String service) {
        return new ModelServiceReadiness(
                service,
                false,
                false,
                "unreachable",
                null,
                false,
                Map.of(),
                "network_or_timeout"
        );
    }
}
