package id.ritmagula.backend.session;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionRetentionScheduler {

    private final DemoSessionService sessionService;

    public SessionRetentionScheduler(DemoSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(
            fixedDelayString = "${ritmagula.retention.cleanup-delay-ms}",
            initialDelayString = "${ritmagula.retention.cleanup-initial-delay-ms}"
    )
    public void purgeExpiredAndResetSessions() {
        sessionService.purgeExpiredAndResetSessions();
    }
}
