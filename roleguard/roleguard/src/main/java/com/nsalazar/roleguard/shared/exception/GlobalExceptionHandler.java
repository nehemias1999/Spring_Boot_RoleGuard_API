package com.nsalazar.roleguard.shared.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for all REST controllers.
 * Converts domain and validation exceptions into structured {@link ErrorResponse} payloads.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles Bean Validation failures from {@code @Valid} on request bodies.
     * All field errors are concatenated into a single message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handles {@code @Min} / {@code @Max} violations on {@code @RequestParam} values
     * (requires {@code @Validated} on the controller class).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    // Strip method name prefix (e.g. "getAllUsers.size" → "size")
                    int dot = path.lastIndexOf('.');
                    return (dot >= 0 ? path.substring(dot + 1) : path) + ": " + v.getMessage();
                })
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handles optimistic locking conflicts raised by {@code @Version}.
     * Occurs when two concurrent requests attempt to update the same entity version.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict on {}: {}", ex.getPersistentClassName(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "The resource was modified by another request. Fetch the latest version and retry.");
    }

    /**
     * Handles invalid sort field names passed to paginated endpoints.
     * Raised when the sort parameter references a field that does not exist on the entity.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSortField(PropertyReferenceException ex) {
        log.warn("Invalid sort field '{}': {}", ex.getPropertyName(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid sort field: '" + ex.getPropertyName() + "'");
    }

    /**
     * Handles FK constraint violations from the database.
     * Typical case: attempting to delete a Role that still has Users assigned to it.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "Cannot complete the operation: the resource is referenced by other entities.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now())
        );
    }
}
