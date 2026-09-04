CREATE TABLE push_task (
    id BIGSERIAL PRIMARY KEY,
    business_type VARCHAR(32) NOT NULL,
    reminder_type VARCHAR(64) NOT NULL,
    reminder_date DATE NOT NULL,
    recipient_wecom_id VARCHAR(128),
    recipient_name VARCHAR(128),
    content TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_push_status CHECK (status IN ('PENDING','RETRYING','SENT','FAILED','CANCELLED'))
);
CREATE UNIQUE INDEX uk_push_task_dedup ON push_task(reminder_type, recipient_wecom_id, reminder_date, business_type);
CREATE INDEX idx_push_task_status ON push_task(status, scheduled_at);
CREATE TABLE push_attempt (id BIGSERIAL PRIMARY KEY, task_id BIGINT NOT NULL REFERENCES push_task(id), attempt_no INTEGER NOT NULL, attempted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, status VARCHAR(24) NOT NULL, error_message TEXT);
