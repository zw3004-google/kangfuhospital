package cn.hospital.rehab.system.department;

import cn.hospital.rehab.system.common.ImportResult;
import com.alibaba.excel.EasyExcel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class DepartmentTransferService {
    private final DepartmentRepository repository;
    private final DepartmentService departments;

    public DepartmentTransferService(DepartmentRepository repository, DepartmentService departments) {
        this.repository = repository;
        this.departments = departments;
    }

    public byte[] template() { return workbook(List.of(), "科室导入模板"); }

    public byte[] exportAll() {
        List<DepartmentImportRow> rows = repository.findAll(null, null).stream().map(item -> {
            DepartmentImportRow row = new DepartmentImportRow();
            row.departmentCode = item.departmentCode();
            row.departmentName = item.departmentName();
            return row;
        }).toList();
        return workbook(rows, "科室");
    }

    @Transactional
    public ImportResult importFile(MultipartFile file) {
        validate(file);
        List<DepartmentImportRow> rows;
        try { rows = EasyExcel.read(file.getInputStream()).head(DepartmentImportRow.class).sheet().doReadSync(); }
        catch (IOException | RuntimeException error) { throw new IllegalArgumentException("Excel读取失败：" + error.getMessage(), error); }
        if (rows.isEmpty()) throw new IllegalArgumentException("导入文件没有数据行");
        int count = 0;
        for (int index = 0; index < rows.size(); index++) {
            DepartmentImportRow row = rows.get(index);
            String code = required(row.departmentCode, index, "科室编码").toUpperCase();
            String name = required(row.departmentName, index, "科室名称");
            if (repository.codeExists(code)) throw new IllegalArgumentException("第" + (index + 2) + "行科室编码已存在：" + code);
            departments.create(new CreateDepartmentRequest(code, name));
            count++;
        }
        return new ImportResult(rows.size(), count);
    }

    private static byte[] workbook(List<?> rows, String sheet) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, DepartmentImportRow.class).sheet(sheet).doWrite(rows);
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
