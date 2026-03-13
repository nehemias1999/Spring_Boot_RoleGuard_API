package com.nsalazar.roleguard.user.application.dto;

import java.time.LocalDateTime;

/**
 * Read-only DTO returned by all User endpoints.
 * Password is intentionally excluded.
 *
 * @param id        unique user identifier
 * @param username  unique display name
 * @param email     unique email address
 * @param roleName  name of the assigned role, or {@code null} if none assigned
 * @param enabled   whether the account is active
 * @param version   optimistic locking version — include when submitting update requests
 *                  to detect concurrent modification conflicts
 * @param createdAt timestamp of account creation
 * @param updatedAt timestamp of last update
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String roleName,
        boolean enabled,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
