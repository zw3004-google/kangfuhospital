ALTER TABLE discharge_record
    ADD COLUMN rehab_doctor_appointment_at TIMESTAMPTZ;

CREATE TABLE discharge_rehab_doctor_consultation (
    id BIGSERIAL PRIMARY KEY,
    encounter_id BIGINT NOT NULL REFERENCES patient_encounter(id),
    appointment_at TIMESTAMPTZ NOT NULL,
    executor_name VARCHAR(128),
    execution_result VARCHAR(1000),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reported_at TIMESTAMPTZ
);

CREATE INDEX idx_rehab_doctor_consultation_encounter
    ON discharge_rehab_doctor_consultation(encounter_id, appointment_at)
    WHERE deleted = FALSE;
