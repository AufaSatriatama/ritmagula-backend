package id.ritmagula.backend.system;

import id.ritmagula.backend.model.health.ModelServiceReadiness;

public record SystemReadinessData(
        boolean applicationReady,
        boolean screeningAvailable,
        boolean foodAnalysisAvailable,
        ModelServiceReadiness risk,
        ModelServiceReadiness food
) {
}
