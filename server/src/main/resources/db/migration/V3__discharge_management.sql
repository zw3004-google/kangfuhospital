CREATE TABLE discharge_record (
    id BIGSERIAL PRIMARY KEY,
    encounter_id BIGINT NOT NULL UNIQUE REFERENCES patient_encounter(id),
    planned_discharge_at TIMESTAMPTZ,
    actual_discharge_at TIMESTAMPTZ,
    is_special_patient BOOLEAN NOT NULL DEFAULT FALSE,
    special_reason TEXT,
    abnormal_codes VARCHAR(256),
    abnormal_reason TEXT,
    follow_up_required BOOLEAN,
    follow_up_day7 TEXT,
    follow_up_day30 TEXT,
    follow_up_day60 TEXT,
    outpatient_appointment_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE discharge_nutrition_consultation (
    id BIGSERIAL PRIMARY KEY, encounter_id BIGINT NOT NULL REFERENCES patient_encounter(id),
    appointment_at TIMESTAMPTZ NOT NULL, deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE discharge_home_rehab_consultation (
    id BIGSERIAL PRIMARY KEY, encounter_id BIGINT NOT NULL REFERENCES patient_encounter(id),
    appointment_at TIMESTAMPTZ NOT NULL, deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_discharge_planned ON discharge_record(planned_discharge_at);
CREATE INDEX idx_discharge_actual ON discharge_record(actual_discharge_at);
