package com.nsalazar.roleguard.role.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for partially updating an existing role.
 * All fields are optional — only non-null values will be applied.
 *
 * @param name new role name; if provided, must follow the same naming convention as on creation
 */
public record UpdateRoleRequest(

        @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
                message = "Role name must start with an uppercase letter and contain only uppercase letters, digits, or underscores")
        String name
) {
}
