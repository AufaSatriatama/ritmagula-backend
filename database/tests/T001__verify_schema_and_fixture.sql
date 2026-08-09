\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    missing_tables text[];
    forbidden_columns text[];
BEGIN
    SELECT array_agg(expected_name ORDER BY expected_name)
    INTO missing_tables
    FROM unnest(ARRAY[
        'demo_session', 'profile', 'daily_observation', 'meal_entry', 'screening_audit'
    ]) AS expected_name
    WHERE to_regclass('ritmagula_app.' || expected_name) IS NULL;

    IF missing_tables IS NOT NULL THEN
        RAISE EXCEPTION 'missing required tables: %', missing_tables;
    END IF;

    SELECT array_agg(table_name || '.' || column_name ORDER BY table_name, column_name)
    INTO forbidden_columns
    FROM information_schema.columns
    WHERE table_schema = 'ritmagula_app'
      AND column_name IN (
          'name', 'email', 'password', 'access_token', 'api_key',
          'raw_image', 'raw_request', 'class_probabilities',
          'dysglycemia_probability', 'result_payload'
      );

    IF forbidden_columns IS NOT NULL THEN
        RAISE EXCEPTION 'forbidden persistence columns found: %', forbidden_columns;
    END IF;
END;
$$;

DO $$
DECLARE
    session_count integer;
    profile_count integer;
    observation_count integer;
    window_1_activity integer;
    window_1_meals integer;
    window_2_activity integer;
    window_2_meals integer;
BEGIN
    SELECT count(*) INTO session_count
    FROM ritmagula_app.demo_session
    WHERE fixture_code = 'RG-P0-01'
      AND data_classification = 'FICTIONAL_DEMO'
      AND expires_at <= created_at + INTERVAL '24 hours';

    SELECT count(*) INTO profile_count
    FROM ritmagula_app.profile
    WHERE session_id = '00000000-0000-4000-8000-000000000001'
      AND age_years = 38
      AND sex_at_birth = 'male'
      AND height_cm = 170
      AND weight_kg = 82
      AND family_history_diabetes = true
      AND hypertension = false
      AND pregnant = false
      AND diagnosed_diabetes = false
      AND taking_diabetes_medication = false;

    SELECT count(*) INTO observation_count
    FROM ritmagula_app.daily_observation
    WHERE session_id = '00000000-0000-4000-8000-000000000001'
      AND observed_on BETWEEN DATE '2026-07-20' AND DATE '2026-08-02';

    SELECT
        count(*) FILTER (
            WHERE observed_on BETWEEN DATE '2026-07-20' AND DATE '2026-07-26'
              AND wear_hours >= 10
              AND (
                  SELECT count(*)
                  FROM jsonb_array_elements(hourly_mims) AS element
                  WHERE jsonb_typeof(element) <> 'null'
              ) >= 18
        ),
        count(*) FILTER (
            WHERE observed_on BETWEEN DATE '2026-07-27' AND DATE '2026-08-02'
              AND wear_hours >= 10
              AND (
                  SELECT count(*)
                  FROM jsonb_array_elements(hourly_mims) AS element
                  WHERE jsonb_typeof(element) <> 'null'
              ) >= 18
        )
    INTO window_1_activity, window_2_activity
    FROM ritmagula_app.daily_observation
    WHERE session_id = '00000000-0000-4000-8000-000000000001';

    SELECT
        count(DISTINCT observation.observed_on) FILTER (
            WHERE observation.observed_on BETWEEN DATE '2026-07-20' AND DATE '2026-07-26'
        ),
        count(DISTINCT observation.observed_on) FILTER (
            WHERE observation.observed_on BETWEEN DATE '2026-07-27' AND DATE '2026-08-02'
        )
    INTO window_1_meals, window_2_meals
    FROM ritmagula_app.meal_entry AS meal
    JOIN ritmagula_app.daily_observation AS observation
      ON observation.id = meal.daily_observation_id
    WHERE observation.session_id = '00000000-0000-4000-8000-000000000001'
      AND meal.confirmed_by_user = true;

    IF session_count <> 1 OR profile_count <> 1 OR observation_count <> 14 THEN
        RAISE EXCEPTION 'fixture identity/count mismatch: session %, profile %, observations %',
            session_count, profile_count, observation_count;
    END IF;

    IF (window_1_activity, window_1_meals, window_2_activity, window_2_meals)
       IS DISTINCT FROM (5, 4, 4, 3) THEN
        RAISE EXCEPTION 'fixture window mismatch: W1 activity/meal %/%, W2 activity/meal %/%',
            window_1_activity, window_1_meals, window_2_activity, window_2_meals;
    END IF;
END;
$$;

DO $$
BEGIN
    BEGIN
        INSERT INTO ritmagula_app.demo_session (
            id, observation_start_date, created_at, expires_at, consented_at
        ) VALUES (
            '10000000-0000-4000-8000-000000000001',
            DATE '2026-07-20',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP + INTERVAL '25 hours',
            CURRENT_TIMESTAMP
        );
        RAISE EXCEPTION 'expected 24-hour session constraint to reject the row';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    BEGIN
        UPDATE ritmagula_app.profile
        SET pregnant = true
        WHERE session_id = '00000000-0000-4000-8000-000000000001';
        RAISE EXCEPTION 'expected profile intended-population constraint to reject the row';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    BEGIN
        INSERT INTO ritmagula_app.daily_observation (
            id, session_id, observed_on, hourly_mims, wear_hours
        ) VALUES (
            '10000000-0000-4000-8000-000000000002',
            '00000000-0000-4000-8000-000000000001',
            DATE '2026-08-03',
            to_jsonb(array_fill(NULL::double precision, ARRAY[24])),
            0
        );
        RAISE EXCEPTION 'expected 14-day interval constraint to reject the row';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    BEGIN
        INSERT INTO ritmagula_app.daily_observation (
            id, session_id, observed_on, hourly_mims, wear_hours
        ) VALUES (
            '10000000-0000-4000-8000-000000000003',
            '00000000-0000-4000-8000-000000000001',
            DATE '2026-07-25',
            '[0, 1]'::jsonb,
            10
        );
        RAISE EXCEPTION 'expected 24-value MIMS constraint to reject the row';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    BEGIN
        INSERT INTO ritmagula_app.meal_entry (
            id, daily_observation_id, meal_time, calories_kcal,
            carbohydrate_g, protein_g, fat_g, source,
            confirmed_by_user, confirmed_at
        ) VALUES (
            '10000000-0000-4000-8000-000000000004',
            md5('rg-p0-01-observation-2026-07-20')::uuid,
            TIME '18:00', 500, 60, 20, 15, 'manual', false, CURRENT_TIMESTAMP
        );
        RAISE EXCEPTION 'expected confirmation constraint to reject the row';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
END;
$$;

DO $$
BEGIN
    INSERT INTO ritmagula_app.screening_audit (
        id,
        session_id,
        request_id,
        application_status,
        model_status,
        requested_at,
        completed_at,
        model_versions
    ) VALUES (
        '10000000-0000-4000-8000-000000000007',
        '00000000-0000-4000-8000-000000000001',
        'database-test-request-ok',
        'RESULT_AVAILABLE',
        'ok',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        '{"risk":"2.0.0-research"}'::jsonb
    );

    BEGIN
        INSERT INTO ritmagula_app.screening_audit (
            id,
            session_id,
            request_id,
            application_status,
            model_status,
            requested_at,
            completed_at
        ) VALUES (
            '10000000-0000-4000-8000-000000000008',
            '00000000-0000-4000-8000-000000000001',
            'database-test-request-invalid-mapping',
            'RESULT_AVAILABLE',
            'abstained',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        RAISE EXCEPTION 'expected screening status mapping constraint to reject the row';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
END;
$$;

DO $$
DECLARE
    target_observation_id uuid := md5('rg-p0-01-observation-2026-07-20')::uuid;
    item integer;
BEGIN
    FOR item IN 2..20 LOOP
        INSERT INTO ritmagula_app.meal_entry (
            id, daily_observation_id, meal_time, calories_kcal,
            carbohydrate_g, protein_g, fat_g, source, source_version,
            confirmed_by_user, confirmed_at
        ) VALUES (
            md5('meal-limit-test-' || item::text)::uuid,
            target_observation_id,
            TIME '00:00' + (item * INTERVAL '30 minutes'),
            1, 1, 1, 1, 'manual', 'constraint-test', true, CURRENT_TIMESTAMP
        );
    END LOOP;

    BEGIN
        INSERT INTO ritmagula_app.meal_entry (
            id, daily_observation_id, meal_time, calories_kcal,
            carbohydrate_g, protein_g, fat_g, source, source_version,
            confirmed_by_user, confirmed_at
        ) VALUES (
            '10000000-0000-4000-8000-000000000005',
            target_observation_id,
            TIME '23:59', 1, 1, 1, 1, 'manual', 'constraint-test', true, CURRENT_TIMESTAMP
        );
        RAISE EXCEPTION 'expected 20-meal daily limit to reject the row';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
END;
$$;

DO $$
DECLARE
    purge_count bigint;
    remaining integer;
BEGIN
    INSERT INTO ritmagula_app.demo_session (
        id, observation_start_date, created_at, expires_at, consented_at
    ) VALUES (
        '10000000-0000-4000-8000-000000000006',
        DATE '2026-07-20',
        CURRENT_TIMESTAMP - INTERVAL '25 hours',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        CURRENT_TIMESTAMP - INTERVAL '25 hours'
    );

    INSERT INTO ritmagula_app.profile (
        session_id, age_years, sex_at_birth, height_cm, weight_kg,
        pregnant, diagnosed_diabetes, taking_diabetes_medication,
        eligibility_confirmed_at
    ) VALUES (
        '10000000-0000-4000-8000-000000000006', 38, 'male', 170, 82,
        false, false, false, CURRENT_TIMESTAMP - INTERVAL '25 hours'
    );

    SELECT ritmagula_app.purge_expired_demo_sessions(CURRENT_TIMESTAMP)
    INTO purge_count;

    SELECT count(*) INTO remaining
    FROM ritmagula_app.profile
    WHERE session_id = '10000000-0000-4000-8000-000000000006';

    IF purge_count < 1 OR remaining <> 0 THEN
        RAISE EXCEPTION 'expired-session purge/cascade failed: purged %, dependent profiles %',
            purge_count, remaining;
    END IF;
END;
$$;

ROLLBACK;

SELECT 'database schema and fictional fixture verification passed' AS result;
