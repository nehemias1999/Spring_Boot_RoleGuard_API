package com.nsalazar.roleguard.permission.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only DTO returned by all Permission endpoints.
 *
 * @param id        unique permission identifier
 * @param name      unique permission name (e.g. "READ_USERS", "DELETE_ROLES")
 * @param version   optimistic locking version
 * @param createdAt timestamp of permission creation
 * @param updatedAt timestamp of last update
 */
public record PermissionResponse(
        UUID id,
        String name,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
