package com.nsalazar.roleguard.shared.exception;

/**
 * Thrown when attempting to delete a resource that is still referenced by other entities.
 * Maps to HTTP 409 Conflict via {@link GlobalExceptionHandler}.
 */
public class ResourceInUseException extends RuntimeException {

    /**
     * @param resource the entity type being deleted (e.g. "Role")
     * @param field    the referencing field (e.g. "users")
     * @param count    how many entities currently hold the reference
     */
    public ResourceInUseException(String resource, String field, long count) {
        super(String.format("%s cannot be deleted: it is still assigned to %d %s", resource, count, field));
    }
}
