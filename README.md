# RitmaGula Backend

Spring Boot application backend and PostgreSQL schema for the RitmaGula research-only mobile product. This repository consumes the protected Python model services through typed HTTP clients; it does not contain or modify model code or weights.

## Stack

- Java 21 and Spring Boot 4.1
- Maven Wrapper
- PostgreSQL with Flyway
- Spring Data JPA, Validation, MVC, and Actuator

## Local configuration

Copy `.env.example` values into your local environment. Never commit real credentials.

```powershell
$env:RITMAGULA_DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/ritmagula"
$env:RITMAGULA_DATABASE_USER = "ritmagula"
$env:RITMAGULA_DATABASE_PASSWORD = "local-only-password"
$env:RITMAGULA_RISK_BASE_URL = "http://127.0.0.1:8000"
$env:RITMAGULA_FOOD_BASE_URL = "http://127.0.0.1:8001"
$env:RITMAGULA_ALLOWED_ORIGINS = "http://127.0.0.1:8081,http://localhost:8081"
```

## Run and verify

```powershell
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

The application listens on `http://127.0.0.1:8080`. The P0 API is under `/api/v1`; actuator health is `/actuator/health`.

Database migration sources and their deterministic fictional fixture checks remain under `database/`. Flyway also loads the application migration from `src/main/resources/db/migration/`.

## Safety boundary

- Research screening only; never diagnosis or treatment.
- React calls this backend, not the protected model APIs directly.
- Model credentials remain server-side.
- No fabricated result is returned when a model is unavailable.
- No raw food image, model request body, probability payload, name, email, password, or API key is stored by the P0 schema.
