-- RitmaGula PostgreSQL application schema, contract v1.0.
--
-- This migration is forward-only. For the local semifinal demo, recovery means
-- recreating the disposable database and replaying versioned migrations. For
-- any future durable environment, restore a verified backup before replaying;
-- do not improvise destructive down-migrations against live data.

BEGIN;

CREATE SCHEMA ritmagula_app;

COMMENT ON SCHEMA ritmagula_app IS
    'Minimal local-first RitmaGula application data; protected model services remain stateless.';

CREATE FUNCTION ritmagula_app.is_valid_hourly_mims(value jsonb)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT CASE
        WHEN value IS NULL OR jsonb_typeof(value) <> 'array' THEN false
        WHEN jsonb_array_length(value) <> 24 THEN false
        ELSE NOT EXISTS (
            SELECT 1
            FROM jsonb_array_elements(value) AS element
            WHERE jsonb_typeof(element) NOT IN ('number', 'null')
        )
    END;
$$;

CREATE FUNCTION ritmagula_app.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TABLE ritmagula_app.demo_session (
    id uuid PRIMARY KEY,
    fixture_code varchar(50),
    data_classification varchar(32) NOT NULL DEFAULT 'FICTIONAL_DEMO',
    lifecycle_state varchar(16) NOT NULL DEFAULT 'ACTIVE',
    observation_start_date date NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    consented_at timestamptz NOT NULL,
    reset_at timestamptz,
    CONSTRAINT demo_session_data_classification_ck
        CHECK (data_classification = 'FICTIONAL_DEMO'),
    CONSTRAINT demo_session_lifecycle_state_ck
        CHECK (lifecycle_state IN ('ACTIVE', 'RESET')),
    CONSTRAINT demo_session_expiry_ck
        CHECK (
            expires_at > created_at
            AND expires_at <= created_at + INTERVAL '24 hours'
        ),
    CONSTRAINT demo_session_consent_ck
        CHECK (consented_at >= created_at AND consented_at <= expires_at),
    CONSTRAINT demo_session_reset_state_ck
        CHECK (
            (lifecycle_state = 'ACTIVE' AND reset_at IS NULL)
            OR
            (lifecycle_state = 'RESET' AND reset_at IS NOT NULL AND reset_at >= created_at)
        )
);

COMMENT ON TABLE ritmagula_app.demo_session IS
    'Opaque, local-only, fictional demo session with a maximum 24-hour lifetime.';
COMMENT ON COLUMN ritmagula_app.demo_session.id IS
    'Opaque session identifier; not an account, password, API key, or stored access token.';
COMMENT ON COLUMN ritmagula_app.demo_session.observation_start_date IS
    'Day one of the single approved 14-day observation interval.';

CREATE INDEX demo_session_active_expiry_idx
    ON ritmagula_app.demo_session (expires_at)
    WHERE lifecycle_state = 'ACTIVE';

CREATE TABLE ritmagula_app.profile (
    session_id uuid PRIMARY KEY
        REFERENCES ritmagula_app.demo_session (id) ON DELETE CASCADE,
    age_years smallint NOT NULL,
    sex_at_birth varchar(8) NOT NULL,
    height_cm numeric(5,2) NOT NULL,
    weight_kg numeric(6,2) NOT NULL,
    family_history_diabetes boolean,
    hypertension boolean,
    pregnant boolean NOT NULL DEFAULT false,
    diagnosed_diabetes boolean NOT NULL DEFAULT false,
    taking_diabetes_medication boolean NOT NULL DEFAULT false,
    eligibility_confirmed_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT profile_age_ck CHECK (age_years BETWEEN 20 AND 60),
    CONSTRAINT profile_sex_ck CHECK (sex_at_birth IN ('female', 'male')),
    CONSTRAINT profile_height_ck CHECK (height_cm BETWEEN 120 AND 230),
    CONSTRAINT profile_weight_ck CHECK (weight_kg BETWEEN 30 AND 300),
    CONSTRAINT profile_intended_population_ck CHECK (
        pregnant = false
        AND diagnosed_diabetes = false
        AND taking_diabetes_medication = false
    )
);

COMMENT ON TABLE ritmagula_app.profile IS
    'Only profile fields required by the protected risk API; no name or free-text medical history.';

CREATE TRIGGER profile_set_updated_at
BEFORE UPDATE ON ritmagula_app.profile
FOR EACH ROW
EXECUTE FUNCTION ritmagula_app.set_updated_at();

CREATE TABLE ritmagula_app.daily_observation (
    id uuid PRIMARY KEY,
    session_id uuid NOT NULL
        REFERENCES ritmagula_app.demo_session (id) ON DELETE CASCADE,
    observed_on date NOT NULL,
    hourly_mims jsonb NOT NULL,
    wear_hours numeric(4,2) NOT NULL,
    steps integer,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT daily_observation_session_date_uq UNIQUE (session_id, observed_on),
    CONSTRAINT daily_observation_hourly_mims_ck
        CHECK (ritmagula_app.is_valid_hourly_mims(hourly_mims)),
    CONSTRAINT daily_observation_wear_hours_ck CHECK (wear_hours BETWEEN 0 AND 24),
    CONSTRAINT daily_observation_steps_ck CHECK (steps IS NULL OR steps BETWEEN 0 AND 100000)
);

COMMENT ON TABLE ritmagula_app.daily_observation IS
    'One day in the approved 14-day interval; excludes raw wearable files, device IDs, and GPS.';
COMMENT ON COLUMN ritmagula_app.daily_observation.hourly_mims IS
    'Exactly 24 JSON values; every element is numeric or null, matching the risk API contract.';

CREATE FUNCTION ritmagula_app.validate_observation_date()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    interval_start date;
BEGIN
    SELECT observation_start_date
    INTO interval_start
    FROM ritmagula_app.demo_session
    WHERE id = NEW.session_id;

    IF interval_start IS NOT NULL
       AND (NEW.observed_on < interval_start OR NEW.observed_on > interval_start + 13) THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'daily_observation_14_day_interval_ck',
            MESSAGE = 'observation date must be inside the session 14-day interval';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER daily_observation_validate_date
BEFORE INSERT OR UPDATE OF session_id, observed_on
ON ritmagula_app.daily_observation
FOR EACH ROW
EXECUTE FUNCTION ritmagula_app.validate_observation_date();

CREATE TRIGGER daily_observation_set_updated_at
BEFORE UPDATE ON ritmagula_app.daily_observation
FOR EACH ROW
EXECUTE FUNCTION ritmagula_app.set_updated_at();

CREATE TABLE ritmagula_app.meal_entry (
    id uuid PRIMARY KEY,
    daily_observation_id uuid NOT NULL
        REFERENCES ritmagula_app.daily_observation (id) ON DELETE CASCADE,
    meal_time time NOT NULL,
    calories_kcal numeric(8,3) NOT NULL,
    carbohydrate_g numeric(8,3) NOT NULL,
    protein_g numeric(8,3) NOT NULL,
    fat_g numeric(8,3) NOT NULL,
    sugar_g numeric(8,3),
    fiber_g numeric(8,3),
    source varchar(16) NOT NULL,
    source_version varchar(100),
    confirmed_by_user boolean NOT NULL,
    confirmed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT meal_entry_calories_ck CHECK (calories_kcal BETWEEN 0 AND 5000),
    CONSTRAINT meal_entry_carbohydrate_ck CHECK (carbohydrate_g BETWEEN 0 AND 800),
    CONSTRAINT meal_entry_protein_ck CHECK (protein_g BETWEEN 0 AND 500),
    CONSTRAINT meal_entry_fat_ck CHECK (fat_g BETWEEN 0 AND 500),
    CONSTRAINT meal_entry_sugar_ck CHECK (sugar_g IS NULL OR sugar_g BETWEEN 0 AND 500),
    CONSTRAINT meal_entry_fiber_ck CHECK (fiber_g IS NULL OR fiber_g BETWEEN 0 AND 200),
    CONSTRAINT meal_entry_source_ck
        CHECK (source IN ('manual', 'barcode', 'ocr', 'food_cv', 'wearable')),
    CONSTRAINT meal_entry_confirmation_ck CHECK (confirmed_by_user = true)
);

COMMENT ON TABLE ritmagula_app.meal_entry IS
    'Only explicitly confirmed nutrient totals and provenance; raw photos and unconfirmed CV output are excluded.';

CREATE INDEX meal_entry_observation_time_idx
    ON ritmagula_app.meal_entry (daily_observation_id, meal_time);

CREATE FUNCTION ritmagula_app.enforce_meal_limit()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    existing_count integer;
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.daily_observation_id = OLD.daily_observation_id THEN
        RETURN NEW;
    END IF;

    PERFORM 1
    FROM ritmagula_app.daily_observation
    WHERE id = NEW.daily_observation_id
    FOR UPDATE;

    SELECT count(*)
    INTO existing_count
    FROM ritmagula_app.meal_entry
    WHERE daily_observation_id = NEW.daily_observation_id;

    IF existing_count >= 20 THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'meal_entry_daily_limit_ck',
            MESSAGE = 'a daily observation may contain at most 20 meals';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER meal_entry_enforce_limit
BEFORE INSERT OR UPDATE OF daily_observation_id
ON ritmagula_app.meal_entry
FOR EACH ROW
EXECUTE FUNCTION ritmagula_app.enforce_meal_limit();

CREATE TRIGGER meal_entry_set_updated_at
BEFORE UPDATE ON ritmagula_app.meal_entry
FOR EACH ROW
EXECUTE FUNCTION ritmagula_app.set_updated_at();

CREATE TABLE ritmagula_app.screening_audit (
    id uuid PRIMARY KEY,
    session_id uuid NOT NULL
        REFERENCES ritmagula_app.demo_session (id) ON DELETE CASCADE,
    request_id varchar(100) NOT NULL UNIQUE,
    application_status varchar(32) NOT NULL,
    model_status varchar(16),
    requested_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at timestamptz,
    model_versions jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT screening_audit_application_status_ck CHECK (
        application_status IN (
            'SCREENING_REQUESTED',
            'RESULT_AVAILABLE',
            'ABSTAINED',
            'MODEL_UNAVAILABLE',
            'UNAUTHORIZED',
            'VALIDATION_ERROR',
            'SERVICE_TIMEOUT',
            'NETWORK_ERROR'
        )
    ),
    CONSTRAINT screening_audit_model_status_ck
        CHECK (model_status IS NULL OR model_status IN ('ok', 'abstained', 'not_ready')),
    CONSTRAINT screening_audit_model_versions_ck
        CHECK (jsonb_typeof(model_versions) = 'object'),
    CONSTRAINT screening_audit_completion_ck CHECK (
        (application_status = 'SCREENING_REQUESTED' AND completed_at IS NULL)
        OR
        (application_status <> 'SCREENING_REQUESTED' AND completed_at IS NOT NULL AND completed_at >= requested_at)
    ),
    CONSTRAINT screening_audit_status_mapping_ck CHECK (
        (application_status = 'RESULT_AVAILABLE' AND model_status = 'ok')
        OR (application_status = 'ABSTAINED' AND model_status = 'abstained')
        OR (application_status = 'MODEL_UNAVAILABLE' AND model_status = 'not_ready')
        OR (application_status = 'SCREENING_REQUESTED' AND model_status IS NULL)
        OR (application_status IN ('UNAUTHORIZED', 'VALIDATION_ERROR', 'SERVICE_TIMEOUT', 'NETWORK_ERROR') AND model_status IS NULL)
    )
);

COMMENT ON TABLE ritmagula_app.screening_audit IS
    'Minimal request/status/version provenance only; raw requests, probabilities, and result payloads are intentionally absent.';

CREATE INDEX screening_audit_session_requested_idx
    ON ritmagula_app.screening_audit (session_id, requested_at DESC);

CREATE FUNCTION ritmagula_app.purge_expired_demo_sessions(
    as_of timestamptz DEFAULT CURRENT_TIMESTAMP
)
RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE
    deleted_count bigint;
BEGIN
    DELETE FROM ritmagula_app.demo_session
    WHERE expires_at <= as_of OR lifecycle_state = 'RESET';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$;

COMMENT ON FUNCTION ritmagula_app.purge_expired_demo_sessions(timestamptz) IS
    'Deletes expired/reset fictional sessions and all dependent rows through ON DELETE CASCADE.';

COMMIT;
