ALTER TABLE discharge_record
    ADD COLUMN is_turnover_patient BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO sys_role(role_code, role_name, built_in)
VALUES ('PRESIDENT', '院长', TRUE)
ON CONFLICT (role_code) DO UPDATE
    SET role_name = EXCLUDED.role_name,
        built_in = TRUE;