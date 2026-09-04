package cn.hospital.rehab.common.config;

import cn.hospital.rehab.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    private final DatabaseUserDetailsService users;
    private final cn.hospital.rehab.system.user.UserRepository userRepository;
    private final ObjectMapper objectMapper;
    public SecurityConfiguration(DatabaseUserDetailsService users,
                                 cn.hospital.rehab.system.user.UserRepository userRepository,
                                 ObjectMapper objectMapper) {
        this.users = users;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/api/system/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/arrears/records/export").hasAnyAuthority("PERM_API_ARREARS_EXPORT", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/arrears/report/export").hasAnyAuthority("PERM_API_ARREARS_EXPORT", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/arrears/push-records", "/api/arrears/push-records/*/attempts").hasAnyAuthority("PERM_API_PUSH_RECORD_VIEW", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/arrears/push-records/retry-batch", "/api/arrears/push-records/*/retry").hasAnyAuthority("PERM_API_PUSH_RETRY", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/arrears/records", "/api/arrears/records/filter-options", "/api/arrears/records/summary", "/api/arrears/records/*/history", "/api/arrears/report/**").hasAnyAuthority("PERM_API_ARREARS_REPORT", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/arrears/records/**").hasAnyAuthority("PERM_API_ARREARS_EDIT", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/discharge/records/export").hasAnyAuthority("PERM_API_DISCHARGE_EXPORT", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/discharge/records", "/api/discharge/records/summary", "/api/discharge/records/filter-options", "/api/discharge/records/*", "/api/discharge/records/*/history", "/api/discharge/analysis/**").hasAnyAuthority("PERM_API_DISCHARGE_ANALYSIS", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/discharge/reminders/preview").hasAnyAuthority("PERM_API_DISCHARGE_REMINDER", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/discharge/reminders/trigger").hasAnyAuthority("PERM_API_DISCHARGE_REMINDER", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/discharge/records/**").hasAnyAuthority("PERM_API_DISCHARGE_EDIT", "PERM_FIELD_ATTENDING_DOCTOR", "PERM_FIELD_OUTPATIENT", "PERM_FIELD_FOLLOW_UP", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/discharge/consultations/**").hasAnyAuthority("PERM_API_DISCHARGE_ANALYSIS", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/discharge/consultations", "/api/discharge/consultations/**").hasAnyAuthority("PERM_API_DISCHARGE_EDIT", "PERM_FIELD_NUTRITION", "PERM_FIELD_HOME_REHAB", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/discharge/consultations/**").hasAnyAuthority("PERM_API_DISCHARGE_EDIT", "PERM_FIELD_NUTRITION", "PERM_FIELD_HOME_REHAB", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/discharge/consultations/**").hasAnyAuthority("PERM_API_DISCHARGE_EDIT", "PERM_FIELD_NUTRITION", "PERM_FIELD_HOME_REHAB", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/system/fee-coefficients/**").hasAnyAuthority("PERM_API_FEE_CONFIG", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/system/fee-coefficients/**", "/api/system/departments/**").hasAnyAuthority("PERM_API_FEE_CONFIG", "PERM_API_DEPT_MANAGE", "ROLE_SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/system/audit-logs/**").hasAnyAuthority("PERM_API_AUDIT_VIEW", "ROLE_SYSTEM_ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    objectMapper.writeValue(response.getOutputStream(),
                            ApiResponse.error("账号或密码错误，或账号已被锁定"));
                }))
                .build();
    }

    @Bean
    org.springframework.security.authentication.dao.DaoAuthenticationProvider authenticationProvider(PasswordEncoder encoder) {
        var provider = new org.springframework.security.authentication.dao.DaoAuthenticationProvider(encoder) {
            @Override protected void additionalAuthenticationChecks(org.springframework.security.core.userdetails.UserDetails user, org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
                try { super.additionalAuthenticationChecks(user, auth); userRepository.clearLoginFailures(user.getUsername()); }
                catch (org.springframework.security.core.AuthenticationException ex) { userRepository.recordLoginFailure(user.getUsername()); throw ex; }
            }
        };
        provider.setUserDetailsService(users);
        return provider;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
