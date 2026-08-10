package id.ritmagula.backend.session;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record CreateDemoSessionRequest(
        @AssertTrue(message = "Persetujuan riset wajib diberikan.") boolean consentAccepted,
        @NotNull(message = "Tanggal awal observasi wajib diisi.") LocalDate observationStartDate,
        @Pattern(regexp = "[A-Za-z0-9._-]{1,50}", message = "Kode fixture tidak valid.") String fixtureCode
) {
}
