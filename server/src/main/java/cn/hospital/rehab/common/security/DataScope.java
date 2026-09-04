package cn.hospital.rehab.common.security;
import java.util.Set;

public record DataScope(boolean allDepartments, Set<Long> departmentIds, Long doctorUserId) {
    public DataScope {
        departmentIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
    }

    public static DataScope all() {
        return new DataScope(true, Set.of(), null);
    }
}
