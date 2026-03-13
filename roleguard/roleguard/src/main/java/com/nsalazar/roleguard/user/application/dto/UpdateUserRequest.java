package com.nsalazar.roleguard.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request payload for partially updating an existing user.
 * All fields are optional — only non-null values will be applied.
 *
 * @param username new username; if provided, must be 3–50 characters and unique
 * @param email    new email; if provided, must be valid and unique
 * @param password new raw password; if provided, must be at least 8 characters and will be re-hashed
 * @param enabled  if provided, enables or disables the account
 */
public record UpdateUserRequest(

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Email(message = "Email must be a valid address")
        String email,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        Boolean enabled
) {
}
