CREATE TABLE import_batch_error (
    id BIGSERIAL PRIMARY KEY,
    import_batch_id BIGINT NOT NULL REFERENCES import_batch(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,
    inpatient_no VARCHAR(64),
    admission_times INTEGER,
    field_name VARCHAR(128),
    original_value TEXT,
    error_code VARCHAR(64) NOT NULL,
    error_message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_import_batch_error_batch ON import_batch_error(import_batch_id,row_number,id);
