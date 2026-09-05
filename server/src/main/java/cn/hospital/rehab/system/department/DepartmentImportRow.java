package cn.hospital.rehab.system.department;

import com.alibaba.excel.annotation.ExcelProperty;

public class DepartmentImportRow {
    @ExcelProperty(value = "科室编码", index = 0) public String departmentCode;
    @ExcelProperty(value = "科室名称", index = 1) public String departmentName;
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String value) { departmentCode = value; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String value) { departmentName = value; }
}
