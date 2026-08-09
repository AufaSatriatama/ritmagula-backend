package id.ritmagula.backend.profile;

import id.ritmagula.backend.api.ApiException;
import id.ritmagula.backend.api.ApplicationCode;
import id.ritmagula.backend.session.DemoSession;
import id.ritmagula.backend.session.DemoSessionService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository repository;
    private final DemoSessionService sessionService;
    private final Clock clock = Clock.systemUTC();

    public ProfileService(ProfileRepository repository, DemoSessionService sessionService) {
        this.repository = repository;
        this.sessionService = sessionService;
    }

    @Transactional
    public ProfileResponse save(UUID sessionId, ProfileRequest request) {
        DemoSession session = sessionService.requireActive(sessionId);
        if (request.outsideIntendedPopulation()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ApplicationCode.VALIDATION_ERROR,
                    "Profil berada di luar populasi screening riset RitmaGula."
            );
        }
        Profile profile = repository.findById(sessionId).orElseGet(() -> new Profile(session.getId()));
        profile.update(request, clock.instant());
        return ProfileResponse.from(repository.save(profile));
    }
}
