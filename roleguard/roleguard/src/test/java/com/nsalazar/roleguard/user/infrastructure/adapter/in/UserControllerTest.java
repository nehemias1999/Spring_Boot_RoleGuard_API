package com.nsalazar.roleguard.user.infrastructure.adapter.in;

import com.nsalazar.roleguard.shared.config.SecurityConfig;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.GlobalExceptionHandler;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("UserController")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IUserUseCase userUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private UserResponse sampleResponse() {
        return new UserResponse(1L, "john", "john@example.com", "ROLE_USER", true, 0L, NOW, NOW);
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

            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].username").value("john"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 400 when page param is negative")
        void shouldReturn400_whenPageIsNegative() throws Exception {
            mockMvc.perform(get("/api/v1/users").param("page", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when size exceeds maximum")
        void shouldReturn400_whenSizeExceedsMax() throws Exception {
            mockMvc.perform(get("/api/v1/users").param("size", "101"))
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
            when(userUseCase.getUserById(1L)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("should return 404 when user does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(userUseCase.getUserById(99L))
                    .thenThrow(new ResourceNotFoundException("User", "id", 99L));

            mockMvc.perform(get("/api/v1/users/99"))
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

            mockMvc.perform(get("/api/v1/users/email/john@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("should return 404 when email not found")
        void shouldReturn404_whenEmailNotFound() throws Exception {
            when(userUseCase.getUserByEmail("unknown@example.com"))
                    .thenThrow(new ResourceNotFoundException("User", "email", "unknown@example.com"));

            mockMvc.perform(get("/api/v1/users/email/unknown@example.com"))
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
        @DisplayName("should return 201 when request is valid")
        void shouldReturn201_whenValid() throws Exception {
            CreateUserRequest request =
                    new CreateUserRequest("john", "john@example.com", "password123", null, null);

            when(userUseCase.createUser(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("john"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("should return 400 when required fields are missing or invalid")
        void shouldReturn400_whenInvalidRequest() throws Exception {
            String invalidBody = """
                    {"username": "", "email": "not-an-email", "password": "short"}
                    """;

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 409 when username or email already exists")
        void shouldReturn409_whenDuplicate() throws Exception {
            CreateUserRequest request =
                    new CreateUserRequest("john", "john@example.com", "password123", null, null);

            when(userUseCase.createUser(any()))
                    .thenThrow(new DuplicateResourceException("User", "username", "john"));

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
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
            UserResponse updated = new UserResponse(1L, "johnny", "john@example.com", "ROLE_USER", true, 1L, NOW, NOW);

            when(userUseCase.updateUser(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(put("/api/v1/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("johnny"))
                    .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        @DisplayName("should return 404 when user does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(userUseCase.updateUser(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("User", "id", 99L));

            mockMvc.perform(put("/api/v1/users/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest(null, null, null, null))))
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
            mockMvc.perform(delete("/api/v1/users/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when user does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User", "id", 99L))
                    .when(userUseCase).deleteUser(99L);

            mockMvc.perform(delete("/api/v1/users/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
