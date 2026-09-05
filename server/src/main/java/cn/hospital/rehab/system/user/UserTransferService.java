package cn.hospital.rehab.system.user;

import cn.hospital.rehab.system.common.ImportResult;
import cn.hospital.rehab.system.department.Department;
import cn.hospital.rehab.system.department.DepartmentRepository;
import com.alibaba.excel.EasyExcel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class UserTransferService {
    private final UserService users;
    private final UserRepository repository;
    private final DepartmentRepository departments;

    public UserTransferService(UserService users, UserRepository repository, DepartmentRepository departments) {
        this.users = users;
        this.repository = repository;
        this.departments = departments;
    }

    public byte[] template() { return workbook(List.of(), UserImportRow.class, "用户导入模板"); }

    public byte[] exportAll() {
        List<UserImportRow> rows = repository.findAll().stream().map(user -> {
            UserImportRow row = new UserImportRow();
            row.displayName = user.displayName();
            row.employeeNo = user.employeeNo();
            row.wecomUserId = user.wecomUserId();
            row.departmentCode = departments.findById(user.departmentId()).map(Department::departmentCode).orElse("");
            return row;
        }).toList();
        return workbook(rows, UserImportRow.class, "用户");
    }

    @Transactional
    public ImportResult importFile(MultipartFile file) {
        validate(file);
        List<UserImportRow> rows;
        try { rows = EasyExcel.read(file.getInputStream()).head(UserImportRow.class).sheet().doReadSync(); }
        catch (IOException | RuntimeException error) { throw new IllegalArgumentException("Excel读取失败：" + error.getMessage(), error); }
        if (rows.isEmpty()) throw new IllegalArgumentException("导入文件没有数据行");
        int count = 0;
        for (int index = 0; index < rows.size(); index++) {
            UserImportRow row = rows.get(index);
            String name = required(row.displayName, index, "姓名");
            String employeeNo = required(row.employeeNo, index, "工号");
            String wecomId = required(row.wecomUserId, index, "企微ID");
            final int rowNumber = index + 2;
            final String departmentCode = required(row.departmentCode, index, "所属科室编码").toUpperCase();
            Department department = departments.findAll(departmentCode, true).stream()
                    .filter(item -> item.departmentCode().equalsIgnoreCase(departmentCode)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("第" + rowNumber + "行所属科室编码不存在或已停用：" + departmentCode));
            users.create(new CreateUserRequest(name, employeeNo, wecomId, department.id()));
            count++;
        }
        return new ImportResult(rows.size(), count);
    }

    private static byte[] workbook(List<?> rows, Class<?> type, String sheet) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, type).sheet(sheet).doWrite(rows);
        return output.toByteArray();
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择Excel文件");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) throw new IllegalArgumentException("仅支持xlsx文件");
    }

    private static String required(String value, int index, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("第" + (index + 2) + "行" + field + "不能为空");
        return value.trim();
    }
}
