package com.nsalazar.roleguard.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for the {@code PUT /api/v1/auth/me/password} endpoint.
 *
 * @param currentPassword the user's current (unhashed) password for verification
 * @param newPassword      the desired new password (min 8 characters); will be BCrypt-hashed
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword
) {
}
