ALTER TABLE discharge_record ADD COLUMN planned_discharge_updated_at TIMESTAMPTZ;
UPDATE discharge_record SET planned_discharge_updated_at = updated_at WHERE planned_discharge_at IS NOT NULL;
