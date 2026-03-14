package com.nsalazar.roleguard.auth.infrastructure.adapter.in;

import com.nsalazar.roleguard.auth.application.dto.AuthResponse;
import com.nsalazar.roleguard.auth.application.dto.ChangePasswordRequest;
import com.nsalazar.roleguard.auth.application.dto.LoginRequest;
import com.nsalazar.roleguard.auth.application.dto.RefreshRequest;
import com.nsalazar.roleguard.auth.application.dto.RegisterRequest;
import com.nsalazar.roleguard.auth.domain.port.in.IAuthUseCase;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST adapter for the Auth bounded context.
 * <p>
 * Public endpoints (no JWT required):
 * <ul>
 *   <li>{@code POST /api/v1/auth/login} — authenticates and returns JWT + refresh token.</li>
 *   <li>{@code POST /api/v1/auth/register} — creates a new user and returns JWT + refresh token.</li>
 *   <li>{@code POST /api/v1/auth/refresh} — exchanges a valid refresh token for a new JWT.</li>
 *   <li>{@code POST /api/v1/auth/logout} — revokes the refresh token.</li>
 * </ul>
 * Authenticated endpoint:
 * <ul>
 *   <li>{@code PUT /api/v1/auth/me/password} — changes the authenticated user's password.</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthUseCase authUseCase;

    /**
     * Authenticates a user and returns a signed JWT and a refresh token.
     *
     * @param request validated login credentials
     * @return 200 with {@link AuthResponse}, or 401 if credentials are invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("POST /api/v1/auth/login — username='{}'", request.username());
        AuthResponse response = authUseCase.login(request);
        log.debug("POST /api/v1/auth/login — token issued for username='{}'", request.username());
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user and returns a signed JWT and a refresh token.
     *
     * @param request validated registration payload
     * @return 201 with {@link AuthResponse}, or 400/409
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("POST /api/v1/auth/register — username='{}'", request.username());
        AuthResponse response = authUseCase.register(request);
        log.debug("POST /api/v1/auth/register — token issued for username='{}'", request.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Issues a new access JWT and rotates the refresh token.
     *
     * @param request contains the refresh token
     * @return 200 with new {@link AuthResponse}, or 401 if the refresh token is invalid/expired
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.debug("POST /api/v1/auth/refresh");
        AuthResponse response = authUseCase.refresh(request);
        log.debug("POST /api/v1/auth/refresh — token refreshed for username='{}'", response.username());
        return ResponseEntity.ok(response);
    }

    /**
     * Revokes the refresh token, effectively logging the user out.
     * The access JWT remains valid until it expires naturally.
     *
     * @param request contains the refresh token to revoke
     * @return 204 No Content, or 401 if the refresh token is invalid/expired
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        log.debug("POST /api/v1/auth/logout");
        authUseCase.logout(request);
        log.debug("POST /api/v1/auth/logout — logout complete");
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param userDetails injected authenticated principal (from JWT)
     * @return 200 with the user's own profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("GET /api/v1/auth/me — username='{}'", userDetails.getUsername());
        UserResponse user = authUseCase.getMe(userDetails.getUsername());
        log.debug("GET /api/v1/auth/me — returning profile for username='{}'", userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    /**
     * Changes the authenticated user's own password.
     * Requires a valid JWT. Verifies the current password before applying the change.
     *
     * @param userDetails injected authenticated principal (from JWT)
     * @param request     current and new passwords
     * @return 204 No Content, or 400 on validation failure, or 401 if current password is wrong
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.debug("PUT /api/v1/auth/me/password — username='{}'", userDetails.getUsername());
        authUseCase.changePassword(userDetails.getUsername(), request);
        log.debug("PUT /api/v1/auth/me/password — password changed for username='{}'", userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
