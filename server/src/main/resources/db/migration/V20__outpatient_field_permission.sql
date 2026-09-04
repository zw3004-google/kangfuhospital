INSERT INTO sys_permission(permission_code,permission_name,permission_type,resource_path,http_method)
VALUES ('FIELD_OUTPATIENT','门诊复诊字段编辑','FIELD',NULL,NULL)
ON CONFLICT(permission_code) DO NOTHING;
