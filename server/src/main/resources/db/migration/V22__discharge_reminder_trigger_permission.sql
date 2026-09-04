INSERT INTO sys_permission(permission_code,permission_name,permission_type,resource_path,http_method)
VALUES ('API_DISCHARGE_REMINDER','预出院提醒手动触发','API','/api/discharge/reminders/trigger','POST')
ON CONFLICT(permission_code) DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code='OPERATIONS' AND p.permission_code='API_DISCHARGE_REMINDER'
ON CONFLICT DO NOTHING;
