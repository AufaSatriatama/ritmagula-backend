package id.ritmagula.backend.model.food;

import java.math.BigDecimal;

public interface FoodModelClient {
    FoodAnalysisResult analyze(
            String requestId,
            byte[] image,
            String filename,
            String contentType,
            BigDecimal plateDiameterCm
    );

    FoodConfirmationResult confirm(String requestId, FoodConfirmationCommand command);
}
