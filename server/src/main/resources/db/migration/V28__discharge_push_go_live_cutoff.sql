-- Existing consultation rows predate the new discharge-message rollout and must remain ineligible.
ALTER TABLE discharge_nutrition_consultation ADD COLUMN reported_at TIMESTAMPTZ;
ALTER TABLE discharge_home_rehab_consultation ADD COLUMN reported_at TIMESTAMPTZ;

CREATE INDEX idx_nutrition_consultation_push_eligible
    ON discharge_nutrition_consultation(reported_at, appointment_at)
    WHERE deleted = FALSE;
CREATE INDEX idx_home_rehab_consultation_push_eligible
    ON discharge_home_rehab_consultation(reported_at, appointment_at)
    WHERE deleted = FALSE;