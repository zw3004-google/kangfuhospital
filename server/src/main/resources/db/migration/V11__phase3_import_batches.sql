ALTER TABLE import_batch
    ADD COLUMN added_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN overwritten_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN skipped_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN summary_status VARCHAR(24) NOT NULL DEFAULT 'PENDING';

ALTER TABLE import_batch ADD CONSTRAINT ck_import_summary_status
    CHECK (summary_status IN ('PENDING', 'READY', 'FAILED'));

CREATE INDEX idx_import_batch_latest_success
    ON import_batch(business_type, finished_at DESC)
    WHERE status = 'SUCCESS';
