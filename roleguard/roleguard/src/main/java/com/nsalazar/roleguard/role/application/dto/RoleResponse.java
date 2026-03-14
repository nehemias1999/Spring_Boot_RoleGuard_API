package com.nsalazar.roleguard.role.application.dto;

import com.nsalazar.roleguard.permission.application.dto.PermissionResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only DTO returned by all Role endpoints.
 *
 * @param id          unique role identifier
 * @param name        unique role name (e.g. "ADMIN", "USER")
 * @param version     optimistic locking version — include when submitting update requests
 *                    to detect concurrent modification conflicts
 * @param createdAt   timestamp of role creation
 * @param updatedAt   timestamp of last update
 * @param permissions permissions currently assigned to this role
 */
public record RoleResponse(
        UUID id,
        String name,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PermissionResponse> permissions
) {
}
