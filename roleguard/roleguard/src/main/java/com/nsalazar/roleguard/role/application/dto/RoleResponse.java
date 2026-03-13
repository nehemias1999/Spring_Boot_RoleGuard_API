package com.nsalazar.roleguard.role.application.dto;

import java.time.LocalDateTime;

/**
 * Read-only DTO returned by all Role endpoints.
 *
 * @param id        unique role identifier
 * @param name      unique role name (e.g. "ADMIN", "USER")
 * @param version   optimistic locking version — include when submitting update requests
 *                  to detect concurrent modification conflicts
 * @param createdAt timestamp of role creation
 * @param updatedAt timestamp of last update
 */
public record RoleResponse(
        Long id,
        String name,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
