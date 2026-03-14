package com.nsalazar.roleguard.auth;

import com.nsalazar.roleguard.auth.application.dto.AuthResponse;
import com.nsalazar.roleguard.auth.application.dto.ChangePasswordRequest;
import com.nsalazar.roleguard.auth.application.dto.LoginRequest;
import com.nsalazar.roleguard.auth.application.dto.RefreshRequest;
import com.nsalazar.roleguard.auth.application.dto.RegisterRequest;
import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests covering the full HTTP request → service → H2 database flow.
 * <p>
 * Tests run in order so that state (registered users, issued tokens) carries forward across
 * scenarios, simulating a realistic multi-step session.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Shared state across ordered tests
    private static String userToken;
    private static String userRefreshToken;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("POST /register — should return 201 with token and refreshToken")
    void register_shouldReturn201WithTokens() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "Password1!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        AuthResponse body = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        userToken = body.token();
        userRefreshToken = body.refreshToken();
    }

    @Test
    @Order(2)
    @DisplayName("POST /register — should return 409 when username already taken")
    void register_shouldReturn409_whenDuplicate() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice2@test.com", "Password1!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @Order(3)
    @DisplayName("POST /register — should return 400 when password is too short")
    void register_shouldReturn400_whenPasswordTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest("bob", "bob@test.com", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("POST /login — should return 200 with token and refreshToken")
    void login_shouldReturn200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest("alice", "Password1!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("alice"))
                .andReturn();

        // Rotate stored tokens: login invalidates the previous refresh token
        AuthResponse body = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        userToken = body.token();
        userRefreshToken = body.refreshToken();
    }

    @Test
    @Order(5)
    @DisplayName("POST /login — should return 401 when password is wrong")
    void login_shouldReturn401_whenBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("alice", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // Protected endpoints
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("GET /users — should return 200 when JWT is valid")
    void getUsers_shouldReturn200_withValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    @DisplayName("GET /users — should return 401 when no JWT provided")
    void getUsers_shouldReturn401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("POST /roles — should return 403 when user has no ADMIN authority")
    void createRole_shouldReturn403_withoutAdminAuthority() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest("USER");

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Refresh Token
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("POST /refresh — should return 200 with new tokens when refresh token is valid")
    void refresh_shouldReturn200WithNewTokens() throws Exception {
        RefreshRequest request = new RefreshRequest(userRefreshToken);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse body = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        userToken = body.token();
        userRefreshToken = body.refreshToken();
    }

    @Test
    @Order(10)
    @DisplayName("POST /refresh — should return 401 when refresh token is invalid")
    void refresh_shouldReturn401_whenInvalidToken() throws Exception {
        RefreshRequest request = new RefreshRequest("non-existent-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Change Password
    // -------------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("PUT /me/password — should return 204 when current password is correct")
    void changePassword_shouldReturn204_whenCorrect() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("Password1!", "NewPassword1!");

        mockMvc.perform(put("/api/v1/auth/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(12)
    @DisplayName("PUT /me/password — should return 401 when current password is wrong")
    void changePassword_shouldReturn401_whenWrongCurrentPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword!", "AnotherPass1!");

        mockMvc.perform(put("/api/v1/auth/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("PUT /me/password — should return 401 when no JWT provided")
    void changePassword_shouldReturn401_withoutToken() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("Password1!", "NewPassword1!");

        mockMvc.perform(put("/api/v1/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Test
    @Order(14)
    @DisplayName("POST /logout — should return 204 when refresh token is valid")
    void logout_shouldReturn204_whenRefreshTokenValid() throws Exception {
        RefreshRequest request = new RefreshRequest(userRefreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(15)
    @DisplayName("POST /refresh — should return 401 after logout (token revoked)")
    void refresh_shouldReturn401_afterLogout() throws Exception {
        RefreshRequest request = new RefreshRequest(userRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Rate Limiting
    // -------------------------------------------------------------------------

    @Test
    @Order(16)
    @DisplayName("POST /login — should return 429 after exceeding rate limit")
    void login_shouldReturn429_afterRateLimitExceeded() throws Exception {
        LoginRequest request = new LoginRequest("alice", "wrongpassword");

        // Exhaust the 10-request bucket
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // 11th request should be rate limited
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }
}
