package com.nsalazar.roleguard.shared.config;

import com.nsalazar.roleguard.shared.security.JwtAccessDeniedHandler;
import com.nsalazar.roleguard.shared.security.JwtAuthEntryPoint;
import com.nsalazar.roleguard.shared.security.JwtAuthFilter;
import com.nsalazar.roleguard.shared.security.RateLimitFilter;
import com.nsalazar.roleguard.shared.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the RoleGuard API.
 * <p>
 * Configures a fully stateless filter chain with JWT-based authentication and
 * role-based access control (RBAC). Key decisions:
 * <ul>
 *   <li>CSRF is disabled — the API is stateless and does not use cookies for authentication.</li>
 *   <li>Sessions are set to {@code STATELESS} — no {@code HttpSession} is created or consulted.</li>
 *   <li>Role names stored in the database (e.g. {@code "ADMIN"}) are used directly as
 *       {@code GrantedAuthority} values without a {@code ROLE_} prefix, matching
 *       {@code hasAuthority("ADMIN")} checks.</li>
 * </ul>
 * </p>
 *
 * <h3>Authorization rules (evaluated in order)</h3>
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Requirement</th></tr>
 *   <tr><td>GET</td><td>/api/v1/auth/me</td><td>Any valid JWT (get own profile)</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/auth/me/**</td><td>Any valid JWT (change own password)</td></tr>
 *   <tr><td>ANY</td><td>/api/v1/auth/**</td><td>Public</td></tr>
 *   <tr><td>ANY</td><td>/api/v1/**</td><td>Any valid JWT (fine-grained control via @PreAuthorize)</td></tr>
 *   <tr><td>Anything else</td><td>—</td><td>Denied</td></tr>
 * </table>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // "me" endpoints require a valid JWT (more specific than the auth/** permit-all below)
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/me/**").authenticated()
                        // All other auth endpoints are public
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
