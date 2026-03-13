package com.nsalazar.roleguard.shared.exception;

import java.time.LocalDateTime;

/**
 * Standardized error payload returned by {@link GlobalExceptionHandler} for all error responses.
 *
 * @param status    HTTP status code
 * @param error     HTTP status reason phrase
 * @param message   human-readable description of the error
 * @param timestamp when the error occurred
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
}
