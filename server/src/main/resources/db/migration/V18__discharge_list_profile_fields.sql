ALTER TABLE patient_encounter
    ADD COLUMN gender VARCHAR(16),
    ADD COLUMN primary_diagnosis VARCHAR(500);

ALTER TABLE discharge_record
    ADD COLUMN follow_up_day7_at TIMESTAMPTZ,
    ADD COLUMN follow_up_day30_at TIMESTAMPTZ,
    ADD COLUMN follow_up_day60_at TIMESTAMPTZ;

