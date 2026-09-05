package cn.hospital.rehab.common.web;
import cn.hospital.rehab.common.api.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class CsrfController {
    @GetMapping("/api/auth/csrf")
    public ResponseEntity<ApiResponse<Token>> csrf(CsrfToken csrf, java.security.Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(new Token(csrf.getHeaderName(), csrf.getToken(), principal == null ? null : principal.getName())));
    }
    public record Token(String headerName, String token, String username) {}
}
