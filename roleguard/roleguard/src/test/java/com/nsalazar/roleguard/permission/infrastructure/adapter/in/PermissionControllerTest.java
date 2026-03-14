package com.nsalazar.roleguard.permission.infrastructure.adapter.in;

import com.nsalazar.roleguard.permission.application.dto.CreatePermissionRequest;
import com.nsalazar.roleguard.permission.application.dto.PermissionResponse;
import com.nsalazar.roleguard.permission.application.dto.UpdatePermissionRequest;
import com.nsalazar.roleguard.permission.domain.port.in.IPermissionUseCase;
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

@WebMvcTest(PermissionController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, RateLimitFilter.class, GlobalExceptionHandler.class})
@DisplayName("PermissionController")
class PermissionControllerTest {

    private static final UUID PERMISSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNKNOWN_ID    = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IPermissionUseCase permissionUseCase;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;
    @MockitoBean private JwtAuthEntryPoint authEntryPoint;
    @MockitoBean private JwtAccessDeniedHandler accessDeniedHandler;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private PermissionResponse sampleResponse() {
        return new PermissionResponse(PERMISSION_ID, "READ_USERS", 0L, NOW, NOW);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/permissions
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/permissions")
    class GetAllPermissions {

        @Test
        @DisplayName("should return 200 with paginated permission list")
        void shouldReturn200WithPaginatedList() throws Exception {
            Page<PermissionResponse> page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1);
            when(permissionUseCase.getAllPermissions(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/permissions").with(user("user").authorities(new SimpleGrantedAuthority("PERMISSION_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("READ_USERS"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 400 when page param is negative")
        void shouldReturn400_whenPageIsNegative() throws Exception {
            mockMvc.perform(get("/api/v1/permissions").param("page", "-1").with(user("user").authorities(new SimpleGrantedAuthority("PERMISSION_READ"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when size exceeds maximum")
        void shouldReturn400_whenSizeExceedsMax() throws Exception {
            mockMvc.perform(get("/api/v1/permissions").param("size", "101").with(user("user").authorities(new SimpleGrantedAuthority("PERMISSION_READ"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/permissions/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/permissions/{id}")
    class GetPermissionById {

        @Test
        @DisplayName("should return 200 when permission exists")
        void shouldReturn200_whenPermissionExists() throws Exception {
            when(permissionUseCase.getPermissionById(PERMISSION_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/permissions/" + PERMISSION_ID).with(user("user").authorities(new SimpleGrantedAuthority("PERMISSION_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PERMISSION_ID.toString()))
                    .andExpect(jsonPath("$.name").value("READ_USERS"));
        }

        @Test
        @DisplayName("should return 404 when permission does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(permissionUseCase.getPermissionById(UNKNOWN_ID))
                    .thenThrow(new ResourceNotFoundException("Permission", "id", UNKNOWN_ID));

            mockMvc.perform(get("/api/v1/permissions/" + UNKNOWN_ID).with(user("user").authorities(new SimpleGrantedAuthority("PERMISSION_READ"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/permissions/name/{name}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/permissions/name/{name}")
    class GetPermissionByName {

        @Test
        @DisplayName("should return 200 when permission name exists")
        void shouldReturn200_whenNameExists() throws Exception {
            when(permissionUseCase.getPermissionByName("READ_USERS")).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/permissions/name/READ_USERS").with(user("user").authorities(new SimpleGrantedAuthority("PERMISSION_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("READ_USERS"));
        }

        @Test
        @DisplayName("should return 404 when permission name not found")
        void shouldReturn404_whenNameNotFound() throws Exception {
            when(permissionUseCase.getPermissionByName("UNKNOWN"))
                    .thenThrow(new ResourceNotFoundException("Permission", "name", "UNKNOWN"));

            mockMvc.perform(get("/api/v1/permissions/name/UNKNOWN").with(user("user").authorities(new SimpleGrantedAuthority("PERMISSION_READ"))))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/permissions
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/permissions")
    class CreatePermission {

        @Test
        @DisplayName("should return 201 when request is valid")
        void shouldReturn201_whenValid() throws Exception {
            CreatePermissionRequest request = new CreatePermissionRequest("READ_USERS");
            when(permissionUseCase.createPermission(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/permissions")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("READ_USERS"));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400_whenNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/v1/permissions")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when name has invalid format")
        void shouldReturn400_whenNameHasInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/v1/permissions")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"invalid-permission\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 when permission name already exists")
        void shouldReturn409_whenDuplicate() throws Exception {
            when(permissionUseCase.createPermission(any()))
                    .thenThrow(new DuplicateResourceException("Permission", "name", "READ_USERS"));

            mockMvc.perform(post("/api/v1/permissions")
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreatePermissionRequest("READ_USERS"))))
                    .andExpect(status().isConflict());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/permissions/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/permissions/{id}")
    class UpdatePermission {

        @Test
        @DisplayName("should return 200 with updated permission")
        void shouldReturn200_whenUpdated() throws Exception {
            UpdatePermissionRequest request = new UpdatePermissionRequest("WRITE_USERS");
            PermissionResponse updated = new PermissionResponse(PERMISSION_ID, "WRITE_USERS", 1L, NOW, NOW);

            when(permissionUseCase.updatePermission(eq(PERMISSION_ID), any())).thenReturn(updated);

            mockMvc.perform(put("/api/v1/permissions/" + PERMISSION_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_UPDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("WRITE_USERS"))
                    .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        @DisplayName("should return 404 when permission does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            when(permissionUseCase.updatePermission(eq(UNKNOWN_ID), any()))
                    .thenThrow(new ResourceNotFoundException("Permission", "id", UNKNOWN_ID));

            mockMvc.perform(put("/api/v1/permissions/" + UNKNOWN_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_UPDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdatePermissionRequest("READ_ROLES"))))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/permissions/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/permissions/{id}")
    class DeletePermission {

        @Test
        @DisplayName("should return 204 when permission is deleted")
        void shouldReturn204_whenDeleted() throws Exception {
            mockMvc.perform(delete("/api/v1/permissions/" + PERMISSION_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_DELETE"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when permission does not exist")
        void shouldReturn404_whenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Permission", "id", UNKNOWN_ID))
                    .when(permissionUseCase).deletePermission(UNKNOWN_ID);

            mockMvc.perform(delete("/api/v1/permissions/" + UNKNOWN_ID)
                            .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_DELETE"))))
                    .andExpect(status().isNotFound());
        }
    }
}
