package com.nsalazar.roleguard.shared.security;

import com.nsalazar.roleguard.shared.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles unauthenticated requests that reach a secured endpoint.
 * <p>
 * Invoked by Spring Security's {@code ExceptionTranslationFilter} when no valid
 * authentication is present. Returns a structured {@link ErrorResponse} JSON body
 * with HTTP 401, matching the format produced by {@link com.nsalazar.roleguard.shared.exception.GlobalExceptionHandler}
 * for all other error responses.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Writes a 401 Unauthorized {@link ErrorResponse} to the HTTP response.
     *
     * @param request       the request that triggered the authentication failure
     * @param response      the response to write the error to
     * @param authException the exception describing why authentication failed
     * @throws IOException if writing to the response fails
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Unauthorized request to '{}' — {}", request.getRequestURI(), authException.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                authException.getMessage(),
                LocalDateTime.now()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
