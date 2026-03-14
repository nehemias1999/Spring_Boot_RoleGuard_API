package com.nsalazar.roleguard.permission.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for partially updating a permission.
 * All fields are optional — only non-null values are applied.
 *
 * @param name new permission name (optional)
 */
public record UpdatePermissionRequest(

        @Size(min = 2, max = 100, message = "Permission name must be between 2 and 100 characters")
        @Pattern(
                regexp = "^[A-Z][A-Z0-9_]*$",
                message = "Permission name must contain only uppercase letters, digits and underscores"
        )
        String name

) {
}
