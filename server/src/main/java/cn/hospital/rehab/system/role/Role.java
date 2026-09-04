package cn.hospital.rehab.system.role;

public record Role(long id, String roleCode, String roleName, boolean builtIn, boolean enabled) {
}
