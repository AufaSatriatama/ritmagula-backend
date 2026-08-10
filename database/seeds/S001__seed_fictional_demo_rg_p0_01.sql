-- Development/test fixture RG-P0-01.
--
-- Fictional, non-personal inputs only. The activity and manual nutrition values
-- are deterministic UI/integration data, not AI output, clinical evidence, or
-- model-validation data. Never cite them as measured outcomes or performance.

BEGIN;

INSERT INTO ritmagula_app.demo_session (
    id,
    fixture_code,
    data_classification,
    lifecycle_state,
    observation_start_date,
    created_at,
    expires_at,
    consented_at
)
VALUES (
    '00000000-0000-4000-8000-000000000001',
    'RG-P0-01',
    'FICTIONAL_DEMO',
    'ACTIVE',
    DATE '2026-07-20',
    transaction_timestamp(),
    transaction_timestamp() + INTERVAL '24 hours',
    transaction_timestamp()
);

INSERT INTO ritmagula_app.profile (
    session_id,
    age_years,
    sex_at_birth,
    height_cm,
    weight_kg,
    waist_circumference_cm,
    family_history_diabetes,
    hypertension,
    pregnant,
    diagnosed_diabetes,
    taking_diabetes_medication,
    eligibility_confirmed_at
)
VALUES (
    '00000000-0000-4000-8000-000000000001',
    38,
    'male',
    170,
    82,
    96,
    true,
    false,
    false,
    false,
    false,
    transaction_timestamp()
);

WITH fixture_days AS (
    SELECT observed_on::date
    FROM generate_series(
        TIMESTAMP '2026-07-20 00:00:00',
        TIMESTAMP '2026-08-02 00:00:00',
        INTERVAL '1 day'
    ) AS observed_on
), classified_days AS (
    SELECT
        observed_on,
        observed_on IN (
            DATE '2026-07-20', DATE '2026-07-21', DATE '2026-07-22',
            DATE '2026-07-23', DATE '2026-07-24', DATE '2026-07-27',
            DATE '2026-07-28', DATE '2026-07-29', DATE '2026-07-30'
        ) AS has_valid_activity
    FROM fixture_days
)
INSERT INTO ritmagula_app.daily_observation (
    id,
    session_id,
    observed_on,
    hourly_mims,
    wear_hours,
    steps
)
SELECT
    md5('rg-p0-01-observation-' || observed_on::text)::uuid,
    '00000000-0000-4000-8000-000000000001',
    observed_on,
    CASE
        WHEN has_valid_activity THEN
            '[0.08,0.05,0.03,0.02,0.02,0.04,0.18,0.42,0.31,0.27,0.24,0.29,0.36,0.33,0.28,0.25,0.34,0.47,0.39,0.30,0.22,0.17,0.12,0.09]'::jsonb
        ELSE to_jsonb(array_fill(NULL::double precision, ARRAY[24]))
    END,
    CASE WHEN has_valid_activity THEN 12.00 ELSE 0.00 END,
    NULL
FROM classified_days;

WITH meal_days(observed_on) AS (
    VALUES
        (DATE '2026-07-20'),
        (DATE '2026-07-21'),
        (DATE '2026-07-22'),
        (DATE '2026-07-23'),
        (DATE '2026-07-27'),
        (DATE '2026-07-28'),
        (DATE '2026-07-29')
)
INSERT INTO ritmagula_app.meal_entry (
    id,
    daily_observation_id,
    meal_time,
    calories_kcal,
    carbohydrate_g,
    protein_g,
    fat_g,
    sugar_g,
    fiber_g,
    source,
    source_version,
    confirmed_by_user,
    confirmed_at
)
SELECT
    md5('rg-p0-01-meal-' || meal_days.observed_on::text)::uuid,
    observation.id,
    TIME '12:00:00',
    600,
    75,
    25,
    20,
    NULL,
    NULL,
    'manual',
    'fixture-rg-p0-01-v1',
    true,
    transaction_timestamp()
FROM meal_days
JOIN ritmagula_app.daily_observation AS observation
  ON observation.session_id = '00000000-0000-4000-8000-000000000001'
 AND observation.observed_on = meal_days.observed_on;

COMMIT;
