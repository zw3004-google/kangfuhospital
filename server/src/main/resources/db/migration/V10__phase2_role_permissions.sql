-- Phase 2 operation permissions. DataScope remains an independent, mandatory filter.
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'MENU_ARREARS','MENU_DISCHARGE',
    'API_ARREARS_EDIT','API_ARREARS_IMPORT','API_ARREARS_EXPORT','API_ARREARS_REPORT',
    'API_DISCHARGE_EDIT','API_DISCHARGE_IMPORT','API_DISCHARGE_EXPORT','API_DISCHARGE_ANALYSIS',
    'API_PUSH_RETRY'
)
WHERE r.role_code='OPERATIONS'
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'MENU_ARREARS','MENU_DISCHARGE',
    'API_ARREARS_EDIT','API_ARREARS_IMPORT','API_ARREARS_EXPORT','API_ARREARS_REPORT',
    'API_DISCHARGE_EDIT','API_DISCHARGE_EXPORT','API_DISCHARGE_ANALYSIS',
    'API_PUSH_RETRY'
)
WHERE r.role_code='FINANCE'
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'MENU_ARREARS','MENU_DISCHARGE',
    'API_ARREARS_EDIT','API_ARREARS_EXPORT','API_ARREARS_REPORT',
    'API_DISCHARGE_EDIT','API_DISCHARGE_EXPORT','API_DISCHARGE_ANALYSIS'
)
WHERE r.role_code IN ('DEPARTMENT_DIRECTOR','ATTENDING_DOCTOR')
ON CONFLICT DO NOTHING;
