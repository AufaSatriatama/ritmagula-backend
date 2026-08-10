BEGIN;

ALTER TABLE ritmagula_app.profile
    ADD COLUMN waist_circumference_cm numeric(5,2),
    ADD CONSTRAINT profile_waist_ck CHECK (
        waist_circumference_cm IS NULL OR waist_circumference_cm BETWEEN 50 AND 200
    );

COMMENT ON COLUMN ritmagula_app.profile.waist_circumference_cm IS
    'Optional waist measurement consumed by risk v2.1; WHtR is derived by the protected model.';

ALTER TABLE ritmagula_app.meal_entry
    ADD COLUMN analysis_request_id varchar(64),
    ADD COLUMN selected_label varchar(100),
    ADD COLUMN display_name varchar(160),
    ADD COLUMN model_versions jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN nutrition_basis jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD CONSTRAINT meal_entry_model_versions_ck CHECK (jsonb_typeof(model_versions) = 'object'),
    ADD CONSTRAINT meal_entry_nutrition_basis_ck CHECK (jsonb_typeof(nutrition_basis) = 'object'),
    ADD CONSTRAINT meal_entry_food_cv_provenance_ck CHECK (
        source <> 'food_cv'
        OR (
            analysis_request_id IS NOT NULL
            AND selected_label IS NOT NULL
            AND display_name IS NOT NULL
            AND source_version IS NOT NULL
            AND confirmed_by_user = true
            AND model_versions <> '{}'::jsonb
            AND nutrition_basis <> '{}'::jsonb
        )
    );

COMMENT ON COLUMN ritmagula_app.meal_entry.analysis_request_id IS
    'Model analysis correlation ID only; the raw image and unconfirmed analysis payload are never persisted.';
COMMENT ON COLUMN ritmagula_app.meal_entry.nutrition_basis IS
    'Non-image TKPI/analog provenance returned only after explicit user confirmation.';

CREATE UNIQUE INDEX meal_entry_food_analysis_uq
    ON ritmagula_app.meal_entry (analysis_request_id)
    WHERE source = 'food_cv';

COMMIT;
