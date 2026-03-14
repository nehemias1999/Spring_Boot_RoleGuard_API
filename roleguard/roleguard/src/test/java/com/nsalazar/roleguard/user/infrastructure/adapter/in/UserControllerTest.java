package com.nsalazar.roleguard.user.infrastructure.adapter.in;

import com.nsalazar.roleguard.shared.config.SecurityConfig;
import com.nsalazar.roleguard.shared.exception.GlobalExceptionHandler;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import com.nsalazar.roleguard.shared.security.JwtAccessDeniedHandler;
import com.nsalazar.roleguard.shared.security.JwtAuthEntryPoint;
import com.nsalazar.roleguard.shared.security.JwtAuthFilter;
import com.nsalazar.roleguard.shared.security.JwtService;
import com.nsalazar.roleguard.shared.security.RateLimitFilter;
import com.nsalazar.roleguard.shared.security.UserDetailsServiceImpl;
import com.nsalazar.roleguard.shared.security.UserPrincipal;
import com.nsalazar.roleguard.shared.security.UserSecurityService;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UpdateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import com.nsalazar.roleguard.user.domain.port.in.IUserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, RateLimitFilter.class, GlobalExceptionHandler.class, UserSecurityService.class})
@DisplayName("UserController")
class UserControllerTest {

    private static final UUID USER_ID    = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ROLE_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNKNOWN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IUserUseCase userUseCase;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;
    @MockitoBean private JwtAuthEntryPoint authEntryPoint;
    @MockitoBean private JwtAccessDeniedHandler accessDeniedHandler;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private UserResponse sampleResponse() {
        return new UserResponse(USER_ID, "john", "john@example.com", "ROLE_USER", true, 0L, NOW, NOW);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllUsers {

        @Test
        @DisplayName("should return 200 with paginated user list")
        void shouldReturn200WithPaginatedList() throws Exception {
            Page<UserResponse> page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1);
            when(userUseCase.getAllUsers(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/users").with(user("user").authorities(new SimpleGrantedAuthority("USER_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].username").value("john"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 400 when page param is negative")
        void shouldReturn400_whenPageIsNegative() throws Exception {
            mockMvc.perform(get("/api/v1/users").param("page", "-1").with(user("user").authorities(new SimpleGrantedAuthority("USER_READ"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when size exceeds maximum")
        void shouldReturn400_whenSizeExceedsMax() throws Exception {
            mockMvc.perform(get("/api/v1/users").param("size", "101").with(user("user").authorities(new SimpleGrantedAuthority("USER_READ"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserById {

        @Test
        @DisplayName("should return 200 when user exists")
        void shouldReturn200_whenUserExists() throws Exception {
            when(userUseCase.getUserById(USER_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/users/" + USER_ID).with(user("user").authorities(new SimpleGrantedAuthority("USER_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("should return 404 when user does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(userUseCase.getUserById(UNKNOWN_ID))
                    .thenThrow(new ResourceNotFoundException("User", "id", UNKNOWN_ID));

            mockMvc.perform(get("/api/v1/users/" + UNKNOWN_ID).with(user("user").authorities(new SimpleGrantedAuthority("USER_READ"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users/email/{email}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/users/email/{email}")
    class GetUserByEmail {

        @Test
        @DisplayName("should return 200 when email exists")
        void shouldReturn200_whenEmailExists() throws Exception {
            when(userUseCase.getUserByEmail("john@example.com")).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/users/email/john@example.com").with(user("user").authorities(new SimpleGrantedAuthority("USER_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("should return 404 when email not found")
        void shouldReturn404_whenEmailNotFound() throws Exception {
            when(userUseCase.getUserByEmail("unknown@example.com"))
                    .thenThrow(new ResourceNotFoundException("User", "email", "unknown@example.com"));

            mockMvc.perform(get("/api/v1/users/email/unknown@example.com").with(user("user").authorities(new SimpleGrantedAuthority("USER_READ"))))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/users
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/users")
    class CreateUser {

        @Test
        @DisplayName("should return 201 with created user")
        void shouldReturn201_whenCreated() throws Exception {
            CreateUserRequest request = new CreateUserRequest("newuser", "new@example.com", "password123", null, null);
            UserResponse created = new UserResponse(UUID.randomUUID(), "newuser", "new@example.com", "USER", true, 0L, NOW, NOW);
            when(userUseCase.createUser(any())).thenReturn(created);

            mockMvc.perform(post("/api/v1/users")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("USER_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("newuser"))
                    .andExpect(jsonPath("$.email").value("new@example.com"));
        }

        @Test
        @DisplayName("should return 409 when username or email already exists")
        void shouldReturn409_whenDuplicate() throws Exception {
            CreateUserRequest request = new CreateUserRequest("john", "john@example.com", "password123", null, null);
            when(userUseCase.createUser(any()))
                    .thenThrow(new DuplicateResourceException("User", "username", "john"));

            mockMvc.perform(post("/api/v1/users")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("USER_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 when request is invalid")
        void shouldReturn400_whenInvalidRequest() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("USER_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"ab\", \"email\": \"not-an-email\", \"password\": \"short\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when caller lacks USER_CREATE authority")
        void shouldReturn403_whenNoAuthority() throws Exception {
            CreateUserRequest request = new CreateUserRequest("newuser", "new@example.com", "password123", null, null);

            mockMvc.perform(post("/api/v1/users")
                            .with(user("regular").authorities(new SimpleGrantedAuthority("USER_READ")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("should return 200 with updated user")
        void shouldReturn200_whenUpdated() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("johnny", null, null, null);
            UserResponse updated = new UserResponse(USER_ID, "johnny", "john@example.com", "ROLE_USER", true, 1L, NOW, NOW);

            when(userUseCase.updateUser(eq(USER_ID), any())).thenReturn(updated);

            mockMvc.perform(put("/api/v1/users/" + USER_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("USER_UPDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("johnny"))
                    .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        @DisplayName("should return 404 when user does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(userUseCase.updateUser(eq(UNKNOWN_ID), any()))
                    .thenThrow(new ResourceNotFoundException("User", "id", UNKNOWN_ID));

            mockMvc.perform(put("/api/v1/users/" + UNKNOWN_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("USER_UPDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest(null, null, null, null))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 200 when caller is updating their own account (no USER_UPDATE authority)")
        void shouldReturn200_whenSelfUpdate() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("johnny", null, null, null);
            UserResponse updated = new UserResponse(USER_ID, "johnny", "john@example.com", "ROLE_USER", true, 1L, NOW, NOW);
            when(userUseCase.updateUser(eq(USER_ID), any())).thenReturn(updated);

            UserPrincipal self = new UserPrincipal(USER_ID, "john", "hashed", true, List.of());

            mockMvc.perform(put("/api/v1/users/" + USER_ID)
                            .with(user(self))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("johnny"));
        }

        @Test
        @DisplayName("should return 403 when caller is a different user without USER_UPDATE authority")
        void shouldReturn403_whenOtherUserWithoutPermission() throws Exception {
            UUID otherUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            UserPrincipal other = new UserPrincipal(otherUserId, "other", "hashed", true, List.of());

            mockMvc.perform(put("/api/v1/users/" + USER_ID)
                            .with(user(other))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest(null, null, null, null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when self-user tries to change enabled field")
        void shouldReturn403_whenSelfTriesToChangeEnabled() throws Exception {
            UserPrincipal self = new UserPrincipal(USER_ID, "john", "hashed", true, List.of());

            mockMvc.perform(put("/api/v1/users/" + USER_ID)
                            .with(user(self))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest(null, null, null, false))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 when USER_UPDATE authority changes enabled field")
        void shouldReturn200_whenAdminChangesEnabled() throws Exception {
            UserResponse updated = new UserResponse(USER_ID, "john", "john@example.com", "ROLE_USER", false, 1L, NOW, NOW);
            when(userUseCase.updateUser(eq(USER_ID), any())).thenReturn(updated);

            mockMvc.perform(put("/api/v1/users/" + USER_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("USER_UPDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest(null, null, null, false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/users/{id}/roles/{roleId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/users/{id}/roles/{roleId}")
    class AssignRole {

        @Test
        @DisplayName("should return 200 with updated user when role is assigned")
        void shouldReturn200_whenRoleAssigned() throws Exception {
            UserResponse withRole = new UserResponse(USER_ID, "john", "john@example.com", "ADMIN", true, 1L, NOW, NOW);
            when(userUseCase.assignRole(USER_ID, ROLE_ID)).thenReturn(withRole);

            mockMvc.perform(put("/api/v1/users/" + USER_ID + "/roles/" + ROLE_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ASSIGN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roleName").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 404 when user or role not found")
        void shouldReturn404_whenNotFound() throws Exception {
            when(userUseCase.assignRole(UNKNOWN_ID, ROLE_ID))
                    .thenThrow(new ResourceNotFoundException("User", "id", UNKNOWN_ID));

            mockMvc.perform(put("/api/v1/users/" + UNKNOWN_ID + "/roles/" + ROLE_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ASSIGN"))))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("should return 204 when user is deleted")
        void shouldReturn204_whenDeleted() throws Exception {
            mockMvc.perform(delete("/api/v1/users/" + USER_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("USER_DELETE"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when user does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User", "id", UNKNOWN_ID))
                    .when(userUseCase).deleteUser(UNKNOWN_ID);

            mockMvc.perform(delete("/api/v1/users/" + UNKNOWN_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("USER_DELETE"))))
                    .andExpect(status().isNotFound());
        }
    }
}
