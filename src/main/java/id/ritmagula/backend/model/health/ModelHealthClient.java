package id.ritmagula.backend.model.health;

@FunctionalInterface
public interface ModelHealthClient {

    ModelServiceReadiness check(String requestId);
}
