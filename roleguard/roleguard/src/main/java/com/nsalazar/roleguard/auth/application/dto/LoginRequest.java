package com.nsalazar.roleguard.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for the {@code POST /api/v1/auth/login} endpoint.
 *
 * @param username account username
 * @param password raw (unhashed) password
 */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
