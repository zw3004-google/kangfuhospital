package cn.hospital.rehab.system.user;

import com.alibaba.excel.annotation.ExcelProperty;

public class UserImportRow {
    @ExcelProperty(value = "姓名", index = 0) public String displayName;
    @ExcelProperty(value = "工号", index = 1) public String employeeNo;
    @ExcelProperty(value = "企微ID", index = 2) public String wecomUserId;
    @ExcelProperty(value = "所属科室编码", index = 3) public String departmentCode;
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String value) { employeeNo = value; }
    public String getWecomUserId() { return wecomUserId; }
    public void setWecomUserId(String value) { wecomUserId = value; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String value) { departmentCode = value; }
}
