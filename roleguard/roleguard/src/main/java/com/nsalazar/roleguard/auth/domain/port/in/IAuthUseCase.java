package com.nsalazar.roleguard.auth.domain.port.in;

import com.nsalazar.roleguard.auth.application.dto.AuthResponse;
import com.nsalazar.roleguard.auth.application.dto.ChangePasswordRequest;
import com.nsalazar.roleguard.auth.application.dto.LoginRequest;
import com.nsalazar.roleguard.auth.application.dto.RefreshRequest;
import com.nsalazar.roleguard.auth.application.dto.RegisterRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;

/**
 * Input port defining all authentication use cases.
 * Controllers depend on this interface, never on the concrete service.
 */
public interface IAuthUseCase {

    /**
     * Authenticates a user and returns a signed JWT plus a refresh token.
     *
     * @param request login credentials
     * @return token response with access JWT, refresh token, token type, username, and role
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the username does not exist or the password is incorrect
     */
    AuthResponse login(LoginRequest request);

    /**
     * Registers a new user and returns a signed JWT immediately.
     * <p>
     * Newly registered users have no role — the {@code role} field in the response
     * will be {@code null} until a role is assigned by an ADMIN.
     * </p>
     *
     * @param request registration details (username, email, password)
     * @return token response with access JWT, refresh token, username, and {@code null} role
     * @throws com.nsalazar.roleguard.shared.exception.DuplicateResourceException
     *         if the username or email is already taken
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Issues a new access JWT in exchange for a valid refresh token.
     * <p>
     * The old refresh token is rotated: a new one is issued and the used one is deleted.
     * </p>
     *
     * @param request contains the refresh token
     * @return new access JWT and a rotated refresh token
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the refresh token is not found or has expired
     */
    AuthResponse refresh(RefreshRequest request);

    /**
     * Invalidates the user's refresh token (logout).
     * The current access JWT remains valid until it expires naturally.
     *
     * @param request contains the refresh token to revoke
     */
    void logout(RefreshRequest request);

    /**
     * Changes the authenticated user's password after verifying the current one.
     *
     * @param username       the authenticated username (extracted from JWT in the controller)
     * @param request        current and new passwords
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if {@code currentPassword} does not match the stored hash
     */
    void changePassword(String username, ChangePasswordRequest request);

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param username the authenticated username (extracted from JWT in the controller)
     * @return the user's profile as a response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    UserResponse getMe(String username);
}
