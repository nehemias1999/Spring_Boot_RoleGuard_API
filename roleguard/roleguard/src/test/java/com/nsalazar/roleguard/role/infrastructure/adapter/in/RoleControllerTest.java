package com.nsalazar.roleguard.role.infrastructure.adapter.in;

import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.application.dto.UpdateRoleRequest;
import com.nsalazar.roleguard.role.domain.port.in.IRoleUseCase;
import com.nsalazar.roleguard.shared.config.SecurityConfig;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.GlobalExceptionHandler;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import com.nsalazar.roleguard.shared.security.JwtAccessDeniedHandler;
import com.nsalazar.roleguard.shared.security.JwtAuthEntryPoint;
import com.nsalazar.roleguard.shared.security.JwtAuthFilter;
import com.nsalazar.roleguard.shared.security.JwtService;
import com.nsalazar.roleguard.shared.security.RateLimitFilter;
import com.nsalazar.roleguard.shared.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import com.nsalazar.roleguard.shared.exception.ResourceInUseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
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

@WebMvcTest(RoleController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, RateLimitFilter.class, GlobalExceptionHandler.class})
@DisplayName("RoleController")
class RoleControllerTest {

    private static final UUID ROLE_ID    = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNKNOWN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IRoleUseCase roleUseCase;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;
    @MockitoBean private JwtAuthEntryPoint authEntryPoint;
    @MockitoBean private JwtAccessDeniedHandler accessDeniedHandler;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static final UUID PERMISSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private RoleResponse sampleResponse() {
        return new RoleResponse(ROLE_ID, "ADMIN", 0L, NOW, NOW, List.of());
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

            mockMvc.perform(get("/api/v1/roles").with(user("user").authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("ADMIN"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 400 when page param is negative")
        void shouldReturn400_whenPageIsNegative() throws Exception {
            mockMvc.perform(get("/api/v1/roles").param("page", "-1").with(user("user").authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when size exceeds maximum")
        void shouldReturn400_whenSizeExceedsMax() throws Exception {
            mockMvc.perform(get("/api/v1/roles").param("size", "101").with(user("user").authorities(new SimpleGrantedAuthority("ROLE_READ"))))
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
            when(roleUseCase.getRoleById(ROLE_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/roles/" + ROLE_ID).with(user("user").authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ROLE_ID.toString()))
                    .andExpect(jsonPath("$.name").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 404 when role does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(roleUseCase.getRoleById(UNKNOWN_ID))
                    .thenThrow(new ResourceNotFoundException("Role", "id", UNKNOWN_ID));

            mockMvc.perform(get("/api/v1/roles/" + UNKNOWN_ID).with(user("user").authorities(new SimpleGrantedAuthority("ROLE_READ"))))
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

            mockMvc.perform(get("/api/v1/roles/name/ADMIN").with(user("user").authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 404 when role name not found")
        void shouldReturn404_whenNameNotFound() throws Exception {
            when(roleUseCase.getRoleByName("UNKNOWN"))
                    .thenThrow(new ResourceNotFoundException("Role", "name", "UNKNOWN"));

            mockMvc.perform(get("/api/v1/roles/name/UNKNOWN").with(user("user").authorities(new SimpleGrantedAuthority("ROLE_READ"))))
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
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400_whenNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/v1/roles")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when name has invalid format")
        void shouldReturn400_whenNameHasInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/v1/roles")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_CREATE")))
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
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_CREATE")))
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
            RoleResponse updated = new RoleResponse(ROLE_ID, "SUPERADMIN", 1L, NOW, NOW, List.of());

            when(roleUseCase.updateRole(eq(ROLE_ID), any())).thenReturn(updated);

            mockMvc.perform(put("/api/v1/roles/" + ROLE_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_UPDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("SUPERADMIN"))
                    .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        @DisplayName("should return 404 when role does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(roleUseCase.updateRole(eq(UNKNOWN_ID), any()))
                    .thenThrow(new ResourceNotFoundException("Role", "id", UNKNOWN_ID));

            mockMvc.perform(put("/api/v1/roles/" + UNKNOWN_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_UPDATE")))
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
            mockMvc.perform(delete("/api/v1/roles/" + ROLE_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_DELETE"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when role does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Role", "id", UNKNOWN_ID))
                    .when(roleUseCase).deleteRole(UNKNOWN_ID);

            mockMvc.perform(delete("/api/v1/roles/" + UNKNOWN_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_DELETE"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when role has users assigned")
        void shouldReturn409_whenRoleHasUsers() throws Exception {
            doThrow(new ResourceInUseException("Role", "user(s)", 2L))
                    .when(roleUseCase).deleteRole(ROLE_ID);

            mockMvc.perform(delete("/api/v1/roles/" + ROLE_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_DELETE"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/roles/{id}/permissions/{permissionId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/roles/{id}/permissions/{permissionId}")
    class AssignPermission {

        @Test
        @DisplayName("should return 200 with updated role when permission is assigned")
        void shouldReturn200_whenPermissionAssigned() throws Exception {
            RoleResponse withPermission = new RoleResponse(ROLE_ID, "ADMIN", 1L, NOW, NOW, List.of());
            when(roleUseCase.assignPermission(ROLE_ID, PERMISSION_ID)).thenReturn(withPermission);

            mockMvc.perform(put("/api/v1/roles/" + ROLE_ID + "/permissions/" + PERMISSION_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_ASSIGN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ROLE_ID.toString()));
        }

        @Test
        @DisplayName("should return 404 when role or permission not found")
        void shouldReturn404_whenNotFound() throws Exception {
            when(roleUseCase.assignPermission(UNKNOWN_ID, PERMISSION_ID))
                    .thenThrow(new ResourceNotFoundException("Role", "id", UNKNOWN_ID));

            mockMvc.perform(put("/api/v1/roles/" + UNKNOWN_ID + "/permissions/" + PERMISSION_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_ASSIGN"))))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/roles/{id}/permissions/{permissionId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/roles/{id}/permissions/{permissionId}")
    class RemovePermission {

        @Test
        @DisplayName("should return 200 with updated role when permission is removed")
        void shouldReturn200_whenPermissionRemoved() throws Exception {
            RoleResponse noPermission = new RoleResponse(ROLE_ID, "ADMIN", 2L, NOW, NOW, List.of());
            when(roleUseCase.removePermission(ROLE_ID, PERMISSION_ID)).thenReturn(noPermission);

            mockMvc.perform(delete("/api/v1/roles/" + ROLE_ID + "/permissions/" + PERMISSION_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_ASSIGN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ROLE_ID.toString()));
        }

        @Test
        @DisplayName("should return 404 when role or permission not found")
        void shouldReturn404_whenNotFound() throws Exception {
            when(roleUseCase.removePermission(UNKNOWN_ID, PERMISSION_ID))
                    .thenThrow(new ResourceNotFoundException("Role", "id", UNKNOWN_ID));

            mockMvc.perform(delete("/api/v1/roles/" + UNKNOWN_ID + "/permissions/" + PERMISSION_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_ASSIGN"))))
                    .andExpect(status().isNotFound());
        }
    }
}
