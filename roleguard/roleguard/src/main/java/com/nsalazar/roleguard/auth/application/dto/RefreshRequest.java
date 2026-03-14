package com.nsalazar.roleguard.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for the {@code POST /api/v1/auth/refresh} and
 * {@code POST /api/v1/auth/logout} endpoints.
 *
 * @param refreshToken the opaque refresh token previously issued by the server
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
