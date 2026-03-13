package com.nsalazar.roleguard.shared.exception;

/**
 * Thrown when a requested resource does not exist in the system.
 * Maps to HTTP 404 Not Found via {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param resource the entity type (e.g. "User", "Role")
     * @param field    the field used for lookup (e.g. "id", "username")
     * @param value    the value that was not found
     */
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
    }
}
