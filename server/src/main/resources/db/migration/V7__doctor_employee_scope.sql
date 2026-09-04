ALTER TABLE patient_encounter ADD COLUMN IF NOT EXISTS doctor_employee_no VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_patient_encounter_doctor_employee ON patient_encounter(doctor_employee_no);
