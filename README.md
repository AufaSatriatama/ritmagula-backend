# RitmaGula Backend

Spring Boot application backend and PostgreSQL schema for the RitmaGula research-only mobile product. This repository consumes the protected Python model services through typed HTTP clients; it does not contain or modify model code or weights.

## Stack

- Java 21 and Spring Boot 4.1
- Maven Wrapper
- PostgreSQL with Flyway
- Spring Data JPA, Validation, MVC, and Actuator

## Local configuration

Copy `.env.example` values into your local environment. Never commit real credentials.

Create the local PostgreSQL role and empty `ritmagula` database first. Flyway creates and validates the application schema when Spring starts. Replace port `5432` below when the local service uses another port such as `5433`.

```powershell
$env:RITMAGULA_DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/ritmagula"
$env:RITMAGULA_DATABASE_USER = "ritmagula"
$env:RITMAGULA_DATABASE_PASSWORD = "local-only-password"
$env:RITMAGULA_BACKEND_ADDRESS = "127.0.0.1"
$env:RITMAGULA_RISK_BASE_URL = "http://127.0.0.1:8000"
$env:RITMAGULA_FOOD_BASE_URL = "http://127.0.0.1:8001"
$env:RITMAGULA_ALLOWED_ORIGINS = "http://127.0.0.1:8081,http://localhost:8081"
```

## Run and verify

```powershell
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

The application listens on `http://127.0.0.1:8080` by default. The loopback binding is intentional for the local fictional-data prototype; do not change it to a LAN/public address without designing authentication and a real-data policy. The P0 API is under `/api/v1`; actuator health is `/actuator/health`.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```

Key local endpoints:

- `POST /api/v1/demo-sessions` creates a consent-gated fictional session.
- `PUT /api/v1/demo-sessions/{sessionId}/profile` accepts the v2.1 profile, including optional `waistCircumferenceCm` (50-200 cm).
- `PUT /api/v1/demo-sessions/{sessionId}/days/{date}/activity` and `POST .../meals` collect confirmed 14-day inputs.
- `GET /api/v1/demo-sessions/{sessionId}/days/{date}/meals` lists only persisted meals for that session and day.
- `GET /api/v1/demo-sessions/{sessionId}/timeline` calculates the two independent 7-day quality windows.
- `POST /api/v1/demo-sessions/{sessionId}/screenings` calls Risk v2.1 and preserves `ok`, `abstained`, timeout, and unavailable states.
- `POST /api/v1/demo-sessions/{sessionId}/food-analyses` accepts JPEG/PNG/WebP multipart uploads up to 10 MB and never persists the raw image.
- `POST /api/v1/demo-sessions/{sessionId}/days/{date}/food-confirmations` requires corrected label, portion, modifiers, and explicit confirmation before persisting the TKPI/analog journal meal.
- `DELETE /api/v1/demo-sessions/{sessionId}` physically removes the session cascade and returns a `SESSION_RESET` JSON envelope.

All success and failure paths use the shared JSON envelope. Validation/malformed JSON return 422, missing API resources return 404, unexpected failures return 500, and reset returns 200; a real embedded HTTP-server regression test covers these responses in addition to MockMvc tests.

Expired sessions are purged on a configurable schedule. Defaults are a 30-second initial delay and a five-minute interval; override them with `RITMAGULA_RETENTION_CLEANUP_INITIAL_DELAY_MS` and `RITMAGULA_RETENTION_CLEANUP_DELAY_MS`.

Database migration sources and their deterministic fictional fixture checks remain under `database/`. Flyway also loads the application migration from `src/main/resources/db/migration/`.

## Safety boundary

- Research screening only; never diagnosis or treatment.
- React calls this backend, not the protected model APIs directly.
- Model credentials remain server-side.
- No fabricated result is returned when a model is unavailable.
- No raw food image, model request body, probability payload, name, email, password, or API key is stored by the P0 schema.
- Every current session is `FICTIONAL_DEMO`; clients must not solicit or submit real personal/health data.
- Food analysis suggestions are not persisted. Only a user-confirmed meal plus non-image request/catalog/model provenance enters the journal, and an analysis cannot be confirmed twice.
- The current protected checkout is expected to report both model services as `not_ready` until approved checksum-matching artifacts are supplied.
