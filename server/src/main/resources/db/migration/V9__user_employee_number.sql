ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS employee_no VARCHAR(64);

-- Existing development users receive a clearly marked migration value so the
-- column can become mandatory without losing records. Administrators must
-- replace these values with the hospital source-system employee number.
UPDATE sys_user
SET employee_no = 'LEGACY-' || id
WHERE employee_no IS NULL OR BTRIM(employee_no) = '';

ALTER TABLE sys_user ALTER COLUMN employee_no SET NOT NULL;
ALTER TABLE sys_user ADD CONSTRAINT uk_sys_user_employee_no UNIQUE (employee_no);

CREATE INDEX IF NOT EXISTS idx_sys_user_employee_enabled
    ON sys_user(employee_no) WHERE enabled = TRUE;
