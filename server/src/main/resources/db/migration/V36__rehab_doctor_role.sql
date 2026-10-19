INSERT INTO sys_role(role_code, role_name, built_in)
VALUES ('REHAB_DOCTOR', '康复医师', TRUE)
ON CONFLICT (role_code) DO UPDATE
    SET role_name = EXCLUDED.role_name,
        built_in = TRUE,
        enabled = TRUE;

INSERT INTO sys_permission(permission_code, permission_name, permission_type, resource_path, http_method)
VALUES ('FIELD_REHAB_DOCTOR', '康复医师字段编辑', 'FIELD', NULL, NULL)
ON CONFLICT (permission_code) DO UPDATE
    SET permission_name = EXCLUDED.permission_name,
        permission_type = EXCLUDED.permission_type,
        enabled = TRUE;

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
  FROM sys_role r
  JOIN sys_permission p ON p.permission_code IN ('MENU_DISCHARGE', 'API_DISCHARGE_ANALYSIS', 'FIELD_REHAB_DOCTOR')
 WHERE r.role_code = 'REHAB_DOCTOR'
ON CONFLICT DO NOTHING;
