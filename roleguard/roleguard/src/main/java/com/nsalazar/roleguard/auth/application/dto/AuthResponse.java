package com.nsalazar.roleguard.auth.application.dto;

/**
 * Response payload returned after successful login, registration, or token refresh.
 *
 * @param token        short-lived access JWT
 * @param type         token type — always {@code "Bearer"}
 * @param username     authenticated username
 * @param role         role name assigned to the user, or {@code null} if none
 * @param refreshToken opaque refresh token used to obtain a new access token via
 *                     {@code POST /api/v1/auth/refresh}; store securely (e.g. HttpOnly cookie)
 */
public record AuthResponse(
        String token,
        String type,
        String username,
        String role,
        String refreshToken
) {
}
