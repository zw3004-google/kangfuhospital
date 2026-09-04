INSERT INTO sys_permission(permission_code,permission_name,permission_type,resource_path,http_method) VALUES
 ('API_ARREARS_IMPORT','欠费数据导入','API','/api/arrears/import','POST'),
 ('API_ARREARS_EXPORT','欠费数据导出','API','/api/arrears/records/export','GET'),
 ('API_ARREARS_REPORT','欠费报表查看','API','/api/arrears/report','GET'),
 ('API_DISCHARGE_IMPORT','预出院数据导入','API','/api/discharge/import','POST'),
 ('API_DISCHARGE_EXPORT','预出院数据导出','API','/api/discharge/records/export','GET'),
 ('API_DISCHARGE_ANALYSIS','预出院统计分析','API','/api/discharge/analysis','GET'),
 ('API_FEE_CONFIG','费别系数配置','API','/api/system/fee-coefficients','*'),
 ('API_DEPT_MANAGE','科室管理','API','/api/system/departments','*'),
 ('API_AUDIT_VIEW','审计日志查看','API','/api/system/audit-logs','GET')
ON CONFLICT(permission_code) DO NOTHING;
