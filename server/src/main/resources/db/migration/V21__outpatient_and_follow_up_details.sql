ALTER TABLE discharge_record
    ADD COLUMN outpatient_arrived BOOLEAN,
    ADD COLUMN outpatient_arrival_at TIMESTAMPTZ,
    ADD COLUMN outpatient_reporter VARCHAR(128),
    ADD COLUMN outpatient_no_show_reason TEXT,
    ADD COLUMN follow_up_details JSONB;
