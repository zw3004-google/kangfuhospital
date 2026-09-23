package cn.hospital.rehab.system.user;

import com.alibaba.excel.annotation.ExcelProperty;

/** 用户管理列表导出行；字段与页面列表一致，不包含操作列。 */
public class UserExportRow {
    @ExcelProperty(value = "姓名", index = 0) public String displayName;
    @ExcelProperty(value = "工号", index = 1) public String employeeNo;
    @ExcelProperty(value = "登录名", index = 2) public String loginName;
    @ExcelProperty(value = "企微ID", index = 3) public String wecomUserId;
    @ExcelProperty(value = "所属科室", index = 4) public String departmentName;
    @ExcelProperty(value = "角色", index = 5) public String roleNames;
    @ExcelProperty(value = "科室权限", index = 6) public String departmentAccessNames;
    @ExcelProperty(value = "状态", index = 7) public String status;

    public String getDisplayName() { return displayName; }
    public String getEmployeeNo() { return employeeNo; }
    public String getLoginName() { return loginName; }
    public String getWecomUserId() { return wecomUserId; }
    public String getDepartmentName() { return departmentName; }
    public String getRoleNames() { return roleNames; }
    public String getDepartmentAccessNames() { return departmentAccessNames; }
    public String getStatus() { return status; }
}