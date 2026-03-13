package com.nsalazar.roleguard.shared.exception;

/**
 * Thrown when attempting to create a resource that would violate a unique constraint.
 * Maps to HTTP 409 Conflict via {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * @param resource the entity type (e.g. "User")
     * @param field    the unique field that conflicts (e.g. "username", "email")
     * @param value    the conflicting value
     */
    public DuplicateResourceException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: '%s'", resource, field, value));
    }
}
