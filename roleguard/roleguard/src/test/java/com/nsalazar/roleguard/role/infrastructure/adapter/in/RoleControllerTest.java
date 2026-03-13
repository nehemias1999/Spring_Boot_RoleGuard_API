package com.nsalazar.roleguard.role.infrastructure.adapter.in;

import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.application.dto.UpdateRoleRequest;
import com.nsalazar.roleguard.role.domain.port.in.IRoleUseCase;
import com.nsalazar.roleguard.shared.config.SecurityConfig;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.GlobalExceptionHandler;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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

@WebMvcTest(RoleController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("RoleController")
class RoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IRoleUseCase roleUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private RoleResponse sampleResponse() {
        return new RoleResponse(1L, "ADMIN", 0L, NOW, NOW);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/roles
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/roles")
    class GetAllRoles {

        @Test
        @DisplayName("should return 200 with paginated role list")
        void shouldReturn200WithPaginatedList() throws Exception {
            Page<RoleResponse> page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1);
            when(roleUseCase.getAllRoles(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("ADMIN"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 400 when page param is negative")
        void shouldReturn400_whenPageIsNegative() throws Exception {
            mockMvc.perform(get("/api/v1/roles").param("page", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when size exceeds maximum")
        void shouldReturn400_whenSizeExceedsMax() throws Exception {
            mockMvc.perform(get("/api/v1/roles").param("size", "101"))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/roles/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/roles/{id}")
    class GetRoleById {

        @Test
        @DisplayName("should return 200 when role exists")
        void shouldReturn200_whenRoleExists() throws Exception {
            when(roleUseCase.getRoleById(1L)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/roles/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 404 when role does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(roleUseCase.getRoleById(99L))
                    .thenThrow(new ResourceNotFoundException("Role", "id", 99L));

            mockMvc.perform(get("/api/v1/roles/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/roles/name/{name}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/roles/name/{name}")
    class GetRoleByName {

        @Test
        @DisplayName("should return 200 when role name exists")
        void shouldReturn200_whenNameExists() throws Exception {
            when(roleUseCase.getRoleByName("ADMIN")).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/roles/name/ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 404 when role name not found")
        void shouldReturn404_whenNameNotFound() throws Exception {
            when(roleUseCase.getRoleByName("UNKNOWN"))
                    .thenThrow(new ResourceNotFoundException("Role", "name", "UNKNOWN"));

            mockMvc.perform(get("/api/v1/roles/name/UNKNOWN"))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/roles
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/roles")
    class CreateRole {

        @Test
        @DisplayName("should return 201 when request is valid")
        void shouldReturn201_whenValid() throws Exception {
            CreateRoleRequest request = new CreateRoleRequest("ADMIN");
            when(roleUseCase.createRole(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400_whenNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/v1/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when name has invalid format")
        void shouldReturn400_whenNameHasInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/v1/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"invalid-role\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 when role name already exists")
        void shouldReturn409_whenDuplicate() throws Exception {
            when(roleUseCase.createRole(any()))
                    .thenThrow(new DuplicateResourceException("Role", "name", "ADMIN"));

            mockMvc.perform(post("/api/v1/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateRoleRequest("ADMIN"))))
                    .andExpect(status().isConflict());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/roles/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/roles/{id}")
    class UpdateRole {

        @Test
        @DisplayName("should return 200 with updated role")
        void shouldReturn200_whenUpdated() throws Exception {
            UpdateRoleRequest request = new UpdateRoleRequest("SUPERADMIN");
            RoleResponse updated = new RoleResponse(1L, "SUPERADMIN", 1L, NOW, NOW);

            when(roleUseCase.updateRole(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(put("/api/v1/roles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("SUPERADMIN"))
                    .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        @DisplayName("should return 404 when role does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(roleUseCase.updateRole(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Role", "id", 99L));

            mockMvc.perform(put("/api/v1/roles/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateRoleRequest("USER"))))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/roles/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/roles/{id}")
    class DeleteRole {

        @Test
        @DisplayName("should return 204 when role is deleted")
        void shouldReturn204_whenDeleted() throws Exception {
            mockMvc.perform(delete("/api/v1/roles/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when role does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Role", "id", 99L))
                    .when(roleUseCase).deleteRole(99L);

            mockMvc.perform(delete("/api/v1/roles/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when role has users assigned")
        void shouldReturn409_whenRoleHasUsers() throws Exception {
            doThrow(new DataIntegrityViolationException("FK constraint violation"))
                    .when(roleUseCase).deleteRole(1L);

            mockMvc.perform(delete("/api/v1/roles/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }
}
