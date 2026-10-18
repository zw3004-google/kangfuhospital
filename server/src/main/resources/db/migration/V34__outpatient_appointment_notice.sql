CREATE TABLE discharge_outpatient_appointment_notice (
    id                  BIGSERIAL PRIMARY KEY,
    discharge_record_id BIGINT NOT NULL REFERENCES discharge_record(id) ON DELETE CASCADE,
    appointment_at      TIMESTAMPTZ NOT NULL,
    configured_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    immediate_notified  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outpatient_appointment_notice_daily
    ON discharge_outpatient_appointment_notice(configured_at, immediate_notified);
