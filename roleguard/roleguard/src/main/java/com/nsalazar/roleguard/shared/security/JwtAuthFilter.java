package com.nsalazar.roleguard.shared.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Stateless JWT authentication filter — runs once per HTTP request.
 * <p>
 * Intercepts the {@code Authorization: Bearer <token>} header, validates the token
 * via {@link JwtService}, and — when valid — populates the {@link SecurityContextHolder}
 * so that downstream security checks see an authenticated principal.
 * </p>
 * <p>
 * Key design decisions:
 * <ul>
 *   <li>Requests without an {@code Authorization} header (or without the {@code Bearer }
 *       prefix) are silently forwarded to the next filter — they may still be served if
 *       the matched route is {@code permitAll()}.</li>
 *   <li>The security context is only updated when it is currently empty, preventing
 *       double-authentication on requests that were already authenticated (e.g. via
 *       Spring Security Test's post-processors).</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Extracts and validates the Bearer JWT from the request.
     * Populates the security context on success; passes through without modification otherwise.
     *
     * @param request     incoming HTTP request
     * @param response    HTTP response
     * @param filterChain remaining filter chain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token in request to '{}' — skipping JWT filter", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (JwtException e) {
            log.warn("Invalid JWT on '{}' — {}", request.getRequestURI(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Validating JWT for username='{}' on '{}'", username, request.getRequestURI());
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(token, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Security context set for username='{}' — authorities={}",
                        username, userDetails.getAuthorities());
            } else {
                log.warn("JWT rejected for username='{}' on '{}'", username, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }
}
