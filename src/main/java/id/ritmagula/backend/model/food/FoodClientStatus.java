package id.ritmagula.backend.model.food;

public enum FoodClientStatus {
    SUCCESS,
    UNAUTHORIZED,
    VALIDATION_ERROR,
    UPLOAD_TOO_LARGE,
    UNAVAILABLE,
    TIMEOUT,
    NETWORK_ERROR,
    INVALID_RESPONSE
}
