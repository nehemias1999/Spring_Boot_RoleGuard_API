package com.nsalazar.roleguard.auth.infrastructure.adapter.in;

import com.nsalazar.roleguard.auth.application.dto.AuthResponse;
import com.nsalazar.roleguard.auth.application.dto.ChangePasswordRequest;
import com.nsalazar.roleguard.auth.application.dto.LoginRequest;
import com.nsalazar.roleguard.auth.application.dto.RefreshRequest;
import com.nsalazar.roleguard.auth.application.dto.RegisterRequest;
import com.nsalazar.roleguard.auth.domain.port.in.IAuthUseCase;
import com.nsalazar.roleguard.shared.config.SecurityConfig;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.GlobalExceptionHandler;
import com.nsalazar.roleguard.shared.security.JwtAccessDeniedHandler;
import com.nsalazar.roleguard.shared.security.JwtAuthEntryPoint;
import com.nsalazar.roleguard.shared.security.JwtAuthFilter;
import com.nsalazar.roleguard.shared.security.JwtService;
import com.nsalazar.roleguard.shared.security.RateLimitFilter;
import com.nsalazar.roleguard.shared.security.UserDetailsServiceImpl;
import com.nsalazar.roleguard.shared.security.UserSecurityService;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, RateLimitFilter.class, GlobalExceptionHandler.class, UserSecurityService.class})
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IAuthUseCase authUseCase;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;
    @MockitoBean private JwtAuthEntryPoint authEntryPoint;
    @MockitoBean private JwtAccessDeniedHandler accessDeniedHandler;

    private static final String TOKEN   = "test.jwt.token";
    private static final String REFRESH = "test-refresh-token";
    private static final UUID   USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private UserResponse sampleUserResponse() {
        return new UserResponse(USER_ID, "admin", "admin@example.com", "ADMIN", true, 0L,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 with token when credentials are valid")
        void shouldReturn200_whenValidCredentials() throws Exception {
            LoginRequest request = new LoginRequest("admin", "password123");
            AuthResponse response = new AuthResponse(TOKEN, "Bearer", "admin", "ADMIN", REFRESH);
            when(authUseCase.login(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.refreshToken").value(REFRESH));
        }

        @Test
        @DisplayName("should return 401 when credentials are invalid")
        void shouldReturn401_whenBadCredentials() throws Exception {
            LoginRequest request = new LoginRequest("admin", "wrong");
            when(authUseCase.login(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("should return 400 when request fields are blank")
        void shouldReturn400_whenBlankFields() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"\", \"password\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 with token when registration is valid")
        void shouldReturn201_whenValidRegistration() throws Exception {
            RegisterRequest request = new RegisterRequest("admin", "admin@test.com", "password123");
            AuthResponse response = new AuthResponse(TOKEN, "Bearer", "admin", null, REFRESH);
            when(authUseCase.register(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.refreshToken").value(REFRESH));
        }

        @Test
        @DisplayName("should return 400 when validation fails")
        void shouldReturn400_whenInvalidRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"ab\", \"email\": \"not-email\", \"password\": \"short\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 409 when username or email already exists")
        void shouldReturn409_whenDuplicateUser() throws Exception {
            RegisterRequest request = new RegisterRequest("admin", "admin@test.com", "password123");
            when(authUseCase.register(any()))
                    .thenThrow(new DuplicateResourceException("User", "username", "admin"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("should return 200 with new token when refresh token is valid")
        void shouldReturn200_whenValidRefreshToken() throws Exception {
            RefreshRequest request = new RefreshRequest(REFRESH);
            AuthResponse response = new AuthResponse(TOKEN, "Bearer", "admin", "ADMIN", "new-refresh-token");
            when(authUseCase.refresh(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
        }

        @Test
        @DisplayName("should return 401 when refresh token is invalid or expired")
        void shouldReturn401_whenInvalidRefreshToken() throws Exception {
            RefreshRequest request = new RefreshRequest("invalid-token");
            when(authUseCase.refresh(any()))
                    .thenThrow(new BadCredentialsException("Invalid refresh token"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when refresh token is blank")
        void shouldReturn400_whenBlankRefreshToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("should return 204 when logout is successful")
        void shouldReturn204_whenLogoutSuccessful() throws Exception {
            RefreshRequest request = new RefreshRequest(REFRESH);
            doNothing().when(authUseCase).logout(any());

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 401 when refresh token is invalid")
        void shouldReturn401_whenInvalidRefreshToken() throws Exception {
            RefreshRequest request = new RefreshRequest("invalid-token");
            doThrow(new BadCredentialsException("Invalid refresh token"))
                    .when(authUseCase).logout(any());

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class GetMe {

        @Test
        @DisplayName("should return 200 with own profile when authenticated")
        void shouldReturn200_whenAuthenticated() throws Exception {
            when(authUseCase.getMe(eq("admin"))).thenReturn(sampleUserResponse());

            mockMvc.perform(get("/api/v1/auth/me").with(user("admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.id").value(USER_ID.toString()));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/auth/me/password")
    class ChangePassword {

        @Test
        @DisplayName("should return 204 when password change is successful")
        void shouldReturn204_whenPasswordChanged() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass1", "newPassword1");
            doNothing().when(authUseCase).changePassword(eq("admin"), any());

            mockMvc.perform(put("/api/v1/auth/me/password")
                            .with(user("admin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 401 when current password is wrong")
        void shouldReturn401_whenCurrentPasswordWrong() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("wrongPass1", "newPassword1");
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authUseCase).changePassword(any(), any());

            mockMvc.perform(put("/api/v1/auth/me/password")
                            .with(user("admin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when new password is too short")
        void shouldReturn400_whenNewPasswordTooShort() throws Exception {
            mockMvc.perform(put("/api/v1/auth/me/password")
                            .with(user("admin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\": \"oldPass1\", \"newPassword\": \"short\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
