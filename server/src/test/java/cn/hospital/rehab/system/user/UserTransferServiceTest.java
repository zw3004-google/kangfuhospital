package cn.hospital.rehab.system.user;

import cn.hospital.rehab.system.department.DepartmentRepository;
import cn.hospital.rehab.system.role.Role;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTransferServiceTest {
    @Test
    void exportsLegacyUserWithoutHomeDepartment() {
        UserRepository users = mock(UserRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        when(users.findAll()).thenReturn(List.of(new UserSummary(1L, "admin", "系统管理员", "ADMIN", "admin",
                null, null, true, false, null, List.of(), List.of(), OffsetDateTime.now())));
        UserTransferService service = new UserTransferService(mock(UserService.class), users, departments);

        assertThat(service.exportAll()).isNotEmpty();
        verify(departments, never()).findById(anyLong());
    }

    @Test
    void exportsOnlyRequestedUsersWhenIdsAreProvided() {
        UserRepository users = mock(UserRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        when(users.findByIds(List.of(2L))).thenReturn(List.of(new UserSummary(2L, "lisi", "李四", "E002", "lisi",
                null, null, true, false, null, List.of(), List.of(), OffsetDateTime.now())));
        UserTransferService service = new UserTransferService(mock(UserService.class), users, departments);

        assertThat(service.export(List.of(2L))).isNotEmpty();
        verify(users).findByIds(List.of(2L));
        verify(users, never()).findAll();
    }
    @Test
    void exportsEveryNonOperationColumnShownInUserList() throws IOException {
        UserRepository users = mock(UserRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        when(users.findAll()).thenReturn(List.of(new UserSummary(3L, "wangwu", "王五", "E003", "wx-wangwu",
                9L, "康复科", false, false, null,
                List.of(new Role(7L, "FOLLOW_UP", "随访员", false, true)),
                List.of("康复科", "后勤管理部"), OffsetDateTime.now())));
        UserTransferService service = new UserTransferService(mock(UserService.class), users, departments);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(service.exportAll()))) {
            var headers = workbook.getSheetAt(0).getRow(0);
            assertThat(headers.getCell(0).getStringCellValue()).isEqualTo("姓名");
            assertThat(headers.getCell(1).getStringCellValue()).isEqualTo("工号");
            assertThat(headers.getCell(2).getStringCellValue()).isEqualTo("登录名");
            assertThat(headers.getCell(3).getStringCellValue()).isEqualTo("企微ID");
            assertThat(headers.getCell(4).getStringCellValue()).isEqualTo("所属科室");
            assertThat(headers.getCell(5).getStringCellValue()).isEqualTo("角色");
            assertThat(headers.getCell(6).getStringCellValue()).isEqualTo("科室权限");
            assertThat(headers.getCell(7).getStringCellValue()).isEqualTo("状态");
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("wangwu");
            assertThat(row.getCell(5).getStringCellValue()).isEqualTo("随访员");
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("康复科、后勤管理部");
            assertThat(row.getCell(7).getStringCellValue()).isEqualTo("停用");
        }
    }
}