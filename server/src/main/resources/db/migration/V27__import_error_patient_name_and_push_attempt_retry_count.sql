ALTER TABLE import_batch_error ADD COLUMN patient_name VARCHAR(128);

ALTER TABLE push_attempt ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;
UPDATE push_attempt SET retry_count=GREATEST(attempt_no-1,0) WHERE retry_count=0;
