package cn.hospital.rehab.system.user;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.system.common.ExcelDownload;
import cn.hospital.rehab.system.common.ImportResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/system/users")
@PreAuthorize("hasAnyAuthority('PERM_API_USER_MANAGE','ROLE_SYSTEM_ADMIN')")
public class UserTransferController {
    private final UserTransferService service;
    public UserTransferController(UserTransferService service) { this.service = service; }

    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        ExcelDownload.write(response, "用户导入模板.xlsx", service.template());
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        ExcelDownload.write(response, "用户导出.xlsx", service.exportAll());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportResult> importFile(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(service.importFile(file));
    }
}
