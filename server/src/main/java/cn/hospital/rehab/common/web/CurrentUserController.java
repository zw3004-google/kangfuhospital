package cn.hospital.rehab.common.web;
import cn.hospital.rehab.common.api.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/system/me")
public class CurrentUserController {
 private final org.springframework.jdbc.core.simple.JdbcClient jdbc; public CurrentUserController(org.springframework.jdbc.core.simple.JdbcClient jdbc){this.jdbc=jdbc;}
 @GetMapping public ApiResponse<CurrentUser> current(Authentication auth){List<String> authorities=auth.getAuthorities().stream().map(a->a.getAuthority()).sorted().toList();boolean must=jdbc.sql("SELECT must_change_password FROM sys_user WHERE login_name=:n").param("n",auth.getName()).query(Boolean.class).single();return ApiResponse.ok(new CurrentUser(auth.getName(),authorities,must));}
 public record CurrentUser(String loginName,List<String> authorities,boolean mustChangePassword){}
}
