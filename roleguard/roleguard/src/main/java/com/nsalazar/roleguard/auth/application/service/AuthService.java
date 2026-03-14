package com.nsalazar.roleguard.auth.application.service;

import com.nsalazar.roleguard.auth.application.dto.AuthResponse;
import com.nsalazar.roleguard.auth.application.dto.ChangePasswordRequest;
import com.nsalazar.roleguard.auth.application.dto.LoginRequest;
import com.nsalazar.roleguard.auth.application.dto.RefreshRequest;
import com.nsalazar.roleguard.auth.application.dto.RegisterRequest;
import com.nsalazar.roleguard.auth.domain.model.RefreshToken;
import com.nsalazar.roleguard.auth.domain.port.in.IAuthUseCase;
import com.nsalazar.roleguard.shared.security.JwtService;
import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UpdateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import com.nsalazar.roleguard.user.domain.port.in.IUserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Application service implementing the authentication use cases.
 * <p>
 * Delegates user creation to {@link IUserUseCase} (reusing all existing validation
 * and password-hashing logic) and credential verification to Spring Security's
 * {@link AuthenticationManager}. All successful operations produce a signed JWT
 * via {@link JwtService} and a refresh token via {@link RefreshTokenService}.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthUseCase {

    private final IUserUseCase userUseCase;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    /**
     * Authenticates a user, issues an access JWT and a refresh token.
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for username='{}'", request.username());
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse(null);
        String token = jwtService.generateToken(request.username(), role);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.username());
        log.info("Login successful for username='{}', role='{}'", request.username(), role);
        return new AuthResponse(token, "Bearer", request.username(), role, refreshToken.getToken());
    }

    /**
     * Registers a new user, issues an access JWT and a refresh token.
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        log.debug("Register attempt for username='{}'", request.username());
        UserResponse user = userUseCase.createUser(
                new CreateUserRequest(request.username(), request.email(), request.password(), null, null)
        );
        String token = jwtService.generateToken(user.username(), user.roleName());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.username());
        log.info("Registration successful for username='{}'", user.username());
        return new AuthResponse(token, "Bearer", user.username(), user.roleName(), refreshToken.getToken());
    }

    /**
     * Validates the provided refresh token, rotates it, and returns a new access JWT.
     */
    @Override
    public AuthResponse refresh(RefreshRequest request) {
        log.debug("Token refresh requested");
        RefreshToken rt = refreshTokenService.validateAndGet(request.refreshToken());
        String username = rt.getUsername();
        UserResponse user = userUseCase.getUserByUsername(username);
        String newAccessToken = jwtService.generateToken(username, user.roleName());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(username);
        log.info("Token refreshed for username='{}'", username);
        return new AuthResponse(newAccessToken, "Bearer", username, user.roleName(), newRefreshToken.getToken());
    }

    /**
     * Revokes the given refresh token (logout). The access JWT remains valid until it expires.
     */
    @Override
    public void logout(RefreshRequest request) {
        log.debug("Logout requested");
        RefreshToken rt = refreshTokenService.validateAndGet(request.refreshToken());
        refreshTokenService.deleteByUsername(rt.getUsername());
        log.info("Logout successful for username='{}'", rt.getUsername());
    }

    /**
     * Verifies the current password and updates it with the new one.
     */
    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        log.debug("Change password requested for username='{}'", username);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.currentPassword())
        );
        UserResponse user = userUseCase.getUserByUsername(username);
        userUseCase.updateUser(user.id(), new UpdateUserRequest(null, null, request.newPassword(), null));
        log.info("Password changed for username='{}'", username);
    }

    /**
     * Returns the profile of the currently authenticated user.
     */
    @Override
    public UserResponse getMe(String username) {
        log.debug("Fetching own profile for username='{}'", username);
        return userUseCase.getUserByUsername(username);
    }
}
