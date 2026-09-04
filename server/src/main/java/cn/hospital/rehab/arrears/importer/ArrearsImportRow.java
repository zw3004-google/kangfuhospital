package cn.hospital.rehab.arrears.importer;

import com.alibaba.excel.annotation.ExcelProperty;

public class ArrearsImportRow {
    @ExcelProperty(value="住院号",index=0) public String inpatientNo;
    @ExcelProperty(value="住院次数",index=1) public Integer admissionTimes;
    @ExcelProperty(value="姓名",index=2) public String patientName;
    @ExcelProperty(value="住院病区",index=3) public String wardName;
    @ExcelProperty(value="费别",index=4) public String feeType;
    @ExcelProperty(value="欠费类型",index=5) public String arrearsType;
    @ExcelProperty(value="主管医生",index=6) public String doctorName;
    @ExcelProperty(value="主管医生工号",index=7) public String doctorEmployeeNo;
    @ExcelProperty(value="入区日期",index=8) public String admittedAt;
    @ExcelProperty(value="出区日期",index=9) public String dischargedAt;
    @ExcelProperty(value="总费用",index=10) public String totalCost;
    @ExcelProperty(value="预交金（元）",index=11) public String prepaidAmount;
    @ExcelProperty(value="医保支付（元）",index=12) public String medicalInsurancePaid;
    @ExcelProperty(value="个人账户支付（元）",index=13) public String personalAccountPaid;
    @ExcelProperty(value="原始应交押金（元）",index=14) public String originalRequiredDeposit;
    public String getInpatientNo(){return inpatientNo;} public void setInpatientNo(String v){inpatientNo=v;}
    public Integer getAdmissionTimes(){return admissionTimes;} public void setAdmissionTimes(Integer v){admissionTimes=v;}
    public String getPatientName(){return patientName;} public void setPatientName(String v){patientName=v;}
    public String getWardName(){return wardName;} public void setWardName(String v){wardName=v;}
    public String getFeeType(){return feeType;} public void setFeeType(String v){feeType=v;}
    public String getArrearsType(){return arrearsType;} public void setArrearsType(String v){arrearsType=v;}
    public String getDoctorName(){return doctorName;} public void setDoctorName(String v){doctorName=v;}
    public String getDoctorEmployeeNo(){return doctorEmployeeNo;} public void setDoctorEmployeeNo(String v){doctorEmployeeNo=v;}
    public String getAdmittedAt(){return admittedAt;} public void setAdmittedAt(String v){admittedAt=v;}
    public String getDischargedAt(){return dischargedAt;} public void setDischargedAt(String v){dischargedAt=v;}
    public String getTotalCost(){return totalCost;} public void setTotalCost(String v){totalCost=v;}
    public String getPrepaidAmount(){return prepaidAmount;} public void setPrepaidAmount(String v){prepaidAmount=v;}
    public String getMedicalInsurancePaid(){return medicalInsurancePaid;} public void setMedicalInsurancePaid(String v){medicalInsurancePaid=v;}
    public String getPersonalAccountPaid(){return personalAccountPaid;} public void setPersonalAccountPaid(String v){personalAccountPaid=v;}
    public String getOriginalRequiredDeposit(){return originalRequiredDeposit;} public void setOriginalRequiredDeposit(String v){originalRequiredDeposit=v;}
}
