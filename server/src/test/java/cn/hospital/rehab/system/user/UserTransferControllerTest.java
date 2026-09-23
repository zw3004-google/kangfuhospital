package cn.hospital.rehab.system.user;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserTransferControllerTest {
    @Test
    void bindsEachSelectedUserIdInsteadOfFallingBackToFullExport() throws Exception {
        UserTransferService service = mock(UserTransferService.class);
        when(service.export(List.of(2L, 5L))).thenReturn(new byte[]{1, 2});
        var mvc = MockMvcBuilders.standaloneSetup(new UserTransferController(service)).build();

        mvc.perform(get("/api/system/users/export").param("ids", "2", "5"))
                .andExpect(status().isOk());

        verify(service).export(List.of(2L, 5L));
    }
}