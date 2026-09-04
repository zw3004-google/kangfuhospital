ALTER TABLE push_task DROP CONSTRAINT ck_push_status;
ALTER TABLE push_task ADD CONSTRAINT ck_push_status
    CHECK (status IN ('PENDING','SENDING','RETRYING','SENT','FAILED','CANCELLED'));
ALTER TABLE push_task ADD COLUMN next_trigger_type VARCHAR(16) NOT NULL DEFAULT 'AUTOMATIC';
ALTER TABLE push_task ADD CONSTRAINT ck_push_trigger_type CHECK (next_trigger_type IN ('AUTOMATIC','MANUAL'));

ALTER TABLE push_attempt
    ADD COLUMN trigger_type VARCHAR(16) NOT NULL DEFAULT 'AUTOMATIC',
    ADD COLUMN scheduled_at TIMESTAMPTZ,
    ADD COLUMN recipient_wecom_id VARCHAR(128),
    ADD COLUMN recipient_name VARCHAR(128),
    ADD COLUMN error_code VARCHAR(64);
ALTER TABLE push_attempt ADD CONSTRAINT ck_push_attempt_trigger CHECK (trigger_type IN ('AUTOMATIC','MANUAL'));
CREATE UNIQUE INDEX uk_push_attempt_no ON push_attempt(task_id,attempt_no);
