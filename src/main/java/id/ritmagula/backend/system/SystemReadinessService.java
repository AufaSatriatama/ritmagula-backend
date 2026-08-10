package id.ritmagula.backend.system;

import id.ritmagula.backend.model.health.ModelHealthClient;
import id.ritmagula.backend.model.health.ModelServiceReadiness;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SystemReadinessService {

    private final ModelHealthClient riskClient;
    private final ModelHealthClient foodClient;

    public SystemReadinessService(
            @Qualifier("riskModelHealthClient") ModelHealthClient riskClient,
            @Qualifier("foodModelHealthClient") ModelHealthClient foodClient
    ) {
        this.riskClient = riskClient;
        this.foodClient = foodClient;
    }

    public SystemReadinessData check(String requestId) {
        ModelServiceReadiness risk = riskClient.check(requestId);
        ModelServiceReadiness food = foodClient.check(requestId);
        return new SystemReadinessData(true, risk.ready(), food.ready(), risk, food);
    }

    public List<String> warnings(SystemReadinessData readiness) {
        List<String> warnings = new ArrayList<>();
        if (!readiness.risk().ready()) {
            warnings.add("Model risiko belum tersedia; hasil screening tidak boleh dibuat.");
        }
        if (!readiness.food().ready()) {
            warnings.add("Food AI belum tersedia; gunakan pencatatan makanan manual.");
        }
        return List.copyOf(warnings);
    }
}
