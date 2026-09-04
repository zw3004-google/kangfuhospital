package cn.hospital.rehab.arrears.api;

import java.util.List;

public record ArrearsFilterOptions(
        List<DepartmentOption> departments,
        List<String> feeTypes,
        List<String> arrearsTypes) {
    public record DepartmentOption(long id, String name) {}
}
