package cn.hospital.rehab.common.security;
import org.springframework.security.access.AccessDeniedException;import org.springframework.security.core.Authentication;import org.springframework.stereotype.Service;
@Service public class FieldPermissionService {public void require(Authentication auth,String code){if(auth==null||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_SYSTEM_ADMIN")||a.getAuthority().equals("PERM_"+code)))throw new AccessDeniedException("无权编辑字段："+code);}}
