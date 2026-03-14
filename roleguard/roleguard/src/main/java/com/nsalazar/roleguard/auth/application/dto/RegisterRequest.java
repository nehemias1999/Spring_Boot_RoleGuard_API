package com.nsalazar.roleguard.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for the {@code POST /api/v1/auth/register} endpoint.
 * <p>
 * Role assignment is intentionally absent — newly registered users start without
 * a role and must be assigned one by an ADMIN via {@code PUT /api/v1/users/{id}}.
 * </p>
 *
 * @param username unique display name (3–50 characters)
 * @param email    unique email address
 * @param password raw password (min 8 characters); will be BCrypt-hashed before persistence
 */
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
