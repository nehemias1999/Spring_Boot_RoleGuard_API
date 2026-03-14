package com.nsalazar.roleguard.shared.security;

import com.nsalazar.roleguard.shared.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles authenticated requests that lack the required authority for an endpoint.
 * <p>
 * Invoked by Spring Security's {@code ExceptionTranslationFilter} when an authenticated
 * user attempts to access a resource they are not authorised for (e.g. a non-ADMIN user
 * calling a write endpoint). Returns a structured {@link ErrorResponse} JSON body with
 * HTTP 403, consistent with the format produced by
 * {@link com.nsalazar.roleguard.shared.exception.GlobalExceptionHandler}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Writes a 403 Forbidden {@link ErrorResponse} to the HTTP response.
     *
     * @param request               the request that triggered the access denial
     * @param response              the response to write the error to
     * @param accessDeniedException the exception describing why access was denied
     * @throws IOException if writing to the response fails
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied for '{}' to '{}' — {}",
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
                request.getRequestURI(),
                accessDeniedException.getMessage());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                accessDeniedException.getMessage(),
                LocalDateTime.now()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
