package com.nsalazar.roleguard.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new user.
 *
 * @param username unique display name (3–50 characters)
 * @param email    unique email address
 * @param password raw password (min 8 characters); will be BCrypt-hashed before persistence
 * @param enabled  whether the account should be active; defaults to {@code true} when not provided
 * @param roleId   optional ID of the role to assign; if null the user is created without a role
 */
public record CreateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        Boolean enabled,

        Long roleId
) {
}
