package com.nsalazar.roleguard.permission.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new permission.
 *
 * @param name unique permission name — uppercase letters, digits and underscores only
 *             (e.g. "READ_USERS", "DELETE_ROLES")
 */
public record CreatePermissionRequest(

        @NotBlank(message = "Permission name must not be blank")
        @Size(min = 2, max = 100, message = "Permission name must be between 2 and 100 characters")
        @Pattern(
                regexp = "^[A-Z][A-Z0-9_]*$",
                message = "Permission name must contain only uppercase letters, digits and underscores"
        )
        String name

) {
}
