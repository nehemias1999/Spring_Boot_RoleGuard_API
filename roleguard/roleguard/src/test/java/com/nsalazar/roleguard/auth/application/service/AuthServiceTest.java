package com.nsalazar.roleguard.auth.application.service;

import com.nsalazar.roleguard.auth.application.dto.AuthResponse;
import com.nsalazar.roleguard.auth.application.dto.ChangePasswordRequest;
import com.nsalazar.roleguard.auth.application.dto.LoginRequest;
import com.nsalazar.roleguard.auth.application.dto.RefreshRequest;
import com.nsalazar.roleguard.auth.application.dto.RegisterRequest;
import com.nsalazar.roleguard.auth.domain.model.RefreshToken;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.security.JwtService;
import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UpdateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import com.nsalazar.roleguard.user.domain.port.in.IUserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private IUserUseCase userUseCase;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String TOKEN = "test.jwt.token";
    private static final String REFRESH = "test-refresh-uuid";

    private RefreshToken refreshToken(String username) {
        return RefreshToken.builder()
                .id(USER_ID)
                .token(REFRESH)
                .username(username)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should return AuthResponse with token and refreshToken when credentials are valid")
        void shouldReturnToken_whenCredentialsValid() {
            LoginRequest request = new LoginRequest("admin", "password123");
            Authentication auth = mock(Authentication.class);
            when(auth.getAuthorities()).thenAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("ADMIN")));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(jwtService.generateToken("admin", "ADMIN")).thenReturn(TOKEN);
            when(refreshTokenService.createRefreshToken("admin")).thenReturn(refreshToken("admin"));

            AuthResponse result = authService.login(request);

            assertThat(result.token()).isEqualTo(TOKEN);
            assertThat(result.username()).isEqualTo("admin");
            assertThat(result.role()).isEqualTo("ADMIN");
            assertThat(result.type()).isEqualTo("Bearer");
            assertThat(result.refreshToken()).isEqualTo(REFRESH);
        }

        @Test
        @DisplayName("should return AuthResponse with null role when user has no role")
        void shouldReturnNullRole_whenNoAuthorities() {
            LoginRequest request = new LoginRequest("admin", "password123");
            Authentication auth = mock(Authentication.class);
            when(auth.getAuthorities()).thenAnswer(inv -> List.of());
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(jwtService.generateToken("admin", null)).thenReturn(TOKEN);
            when(refreshTokenService.createRefreshToken("admin")).thenReturn(refreshToken("admin"));

            AuthResponse result = authService.login(request);

            assertThat(result.role()).isNull();
            assertThat(result.refreshToken()).isEqualTo(REFRESH);
        }

        @Test
        @DisplayName("should propagate BadCredentialsException when credentials are invalid")
        void shouldThrow_whenBadCredentials() {
            LoginRequest request = new LoginRequest("admin", "wrong");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should create user and return AuthResponse when request is valid")
        void shouldRegisterUser_whenValid() {
            RegisterRequest request = new RegisterRequest("admin", "admin@test.com", "password123");
            UserResponse userResponse = new UserResponse(USER_ID, "admin", "admin@test.com", null,
                    true, 0L, LocalDateTime.now(), null);
            when(userUseCase.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);
            when(jwtService.generateToken("admin", null)).thenReturn(TOKEN);
            when(refreshTokenService.createRefreshToken("admin")).thenReturn(refreshToken("admin"));

            AuthResponse result = authService.register(request);

            assertThat(result.token()).isEqualTo(TOKEN);
            assertThat(result.username()).isEqualTo("admin");
            assertThat(result.role()).isNull();
            assertThat(result.type()).isEqualTo("Bearer");
            assertThat(result.refreshToken()).isEqualTo(REFRESH);
        }

        @Test
        @DisplayName("should propagate DuplicateResourceException when username already exists")
        void shouldThrow_whenDuplicateUsername() {
            RegisterRequest request = new RegisterRequest("admin", "admin@test.com", "password123");
            when(userUseCase.createUser(any()))
                    .thenThrow(new DuplicateResourceException("User", "username", "admin"));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("username");
        }

        @Test
        @DisplayName("should propagate DuplicateResourceException when email already exists")
        void shouldThrow_whenDuplicateEmail() {
            RegisterRequest request = new RegisterRequest("admin", "admin@test.com", "password123");
            when(userUseCase.createUser(any()))
                    .thenThrow(new DuplicateResourceException("User", "email", "admin@test.com"));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");
        }
    }

    @Nested
    @DisplayName("refresh()")
    class TokenRefresh {

        @Test
        @DisplayName("should return new tokens when refresh token is valid")
        void shouldReturnNewTokens_whenRefreshTokenValid() {
            RefreshRequest request = new RefreshRequest(REFRESH);
            RefreshToken rt = refreshToken("admin");
            UserResponse userResponse = new UserResponse(USER_ID, "admin", "admin@test.com", "ADMIN",
                    true, 0L, LocalDateTime.now(), null);
            when(refreshTokenService.validateAndGet(REFRESH)).thenReturn(rt);
            when(userUseCase.getUserByUsername("admin")).thenReturn(userResponse);
            when(jwtService.generateToken("admin", "ADMIN")).thenReturn(TOKEN);
            when(refreshTokenService.createRefreshToken("admin")).thenReturn(refreshToken("admin"));

            AuthResponse result = authService.refresh(request);

            assertThat(result.token()).isEqualTo(TOKEN);
            assertThat(result.username()).isEqualTo("admin");
            assertThat(result.refreshToken()).isEqualTo(REFRESH);
        }

        @Test
        @DisplayName("should propagate BadCredentialsException when refresh token is invalid")
        void shouldThrow_whenRefreshTokenInvalid() {
            RefreshRequest request = new RefreshRequest("bad-token");
            when(refreshTokenService.validateAndGet("bad-token"))
                    .thenThrow(new BadCredentialsException("Invalid refresh token"));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("should delete refresh token on logout")
        void shouldDeleteRefreshToken_onLogout() {
            RefreshRequest request = new RefreshRequest(REFRESH);
            RefreshToken rt = refreshToken("admin");
            when(refreshTokenService.validateAndGet(REFRESH)).thenReturn(rt);

            authService.logout(request);

            verify(refreshTokenService).deleteByUsername("admin");
        }

        @Test
        @DisplayName("should propagate BadCredentialsException when refresh token is invalid")
        void shouldThrow_whenRefreshTokenInvalid() {
            RefreshRequest request = new RefreshRequest("bad-token");
            when(refreshTokenService.validateAndGet("bad-token"))
                    .thenThrow(new BadCredentialsException("Invalid refresh token"));

            assertThatThrownBy(() -> authService.logout(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("should verify current password and update to new one")
        void shouldChangePassword_whenCurrentPasswordValid() {
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass1", "newPass123");
            UserResponse userResponse = new UserResponse(USER_ID, "admin", "admin@test.com", "ADMIN",
                    true, 0L, LocalDateTime.now(), null);
            Authentication auth = mock(Authentication.class);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userUseCase.getUserByUsername("admin")).thenReturn(userResponse);
            when(userUseCase.updateUser(eq(USER_ID), any(UpdateUserRequest.class))).thenReturn(userResponse);

            authService.changePassword("admin", request);

            verify(authenticationManager).authenticate(any());
            verify(userUseCase).updateUser(eq(USER_ID), any(UpdateUserRequest.class));
        }

        @Test
        @DisplayName("should propagate BadCredentialsException when current password is wrong")
        void shouldThrow_whenCurrentPasswordWrong() {
            ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass123");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.changePassword("admin", request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
