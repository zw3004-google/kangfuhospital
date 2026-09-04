INSERT INTO sys_permission(
    permission_code, permission_name, permission_type, resource_path, http_method)
VALUES (
    'API_PUSH_RECORD_VIEW', '推送记录查看', 'API', '/api/arrears/push-records', 'GET')
ON CONFLICT(permission_code) DO UPDATE SET
    permission_name=EXCLUDED.permission_name,
    permission_type=EXCLUDED.permission_type,
    resource_path=EXCLUDED.resource_path,
    http_method=EXCLUDED.http_method,
    enabled=TRUE;

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
  FROM sys_role r
  JOIN sys_permission p ON p.permission_code='API_PUSH_RECORD_VIEW'
 WHERE r.role_code IN ('OPERATIONS', 'FINANCE', 'DEPARTMENT_DIRECTOR', 'ATTENDING_DOCTOR')
ON CONFLICT DO NOTHING;
