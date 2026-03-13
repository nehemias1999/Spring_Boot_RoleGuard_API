package com.nsalazar.roleguard.role.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new role.
 *
 * @param name unique role name; uppercase letters and underscores only (e.g. "ADMIN", "SUPER_USER")
 */
public record CreateRoleRequest(

        @NotBlank(message = "Role name is required")
        @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
                message = "Role name must start with an uppercase letter and contain only uppercase letters, digits, or underscores")
        String name
) {
}
