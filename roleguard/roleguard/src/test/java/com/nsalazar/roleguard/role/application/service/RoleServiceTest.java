package com.nsalazar.roleguard.role.application.service;

import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.application.dto.UpdateRoleRequest;
import com.nsalazar.roleguard.role.application.mapper.IRoleMapper;
import com.nsalazar.roleguard.role.domain.model.Role;
import com.nsalazar.roleguard.role.domain.port.out.IRoleRepositoryPort;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService")
class RoleServiceTest {

    @Mock private IRoleRepositoryPort roleRepository;
    @Mock private IRoleMapper roleMapper;

    @InjectMocks private RoleService roleService;

    private Role role;
    private RoleResponse roleResponse;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(1L)
                .name("ADMIN")
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        roleResponse = new RoleResponse(1L, "ADMIN", 0L, role.getCreatedAt(), role.getUpdatedAt());
    }

    // -------------------------------------------------------------------------
    // createRole
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createRole()")
    class CreateRole {

        private final CreateRoleRequest request = new CreateRoleRequest("ADMIN");

        @Test
        @DisplayName("should persist and return RoleResponse when input is valid")
        void shouldCreateRole_whenValidRequest() {
            when(roleRepository.existsByName("ADMIN")).thenReturn(false);
            when(roleMapper.toEntity(request)).thenReturn(role);
            when(roleRepository.save(role)).thenReturn(role);
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            RoleResponse result = roleService.createRole(request);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("ADMIN");
            verify(roleRepository).save(role);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when name already exists")
        void shouldThrow_whenNameExists() {
            when(roleRepository.existsByName("ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> roleService.createRole(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("name");

            verify(roleRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // getRoleById
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getRoleById()")
    class GetRoleById {

        @Test
        @DisplayName("should return RoleResponse when role exists")
        void shouldReturnRole_whenExists() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            assertThat(roleService.getRoleById(1L).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getRoleById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role");
        }
    }

    // -------------------------------------------------------------------------
    // getRoleByName
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getRoleByName()")
    class GetRoleByName {

        @Test
        @DisplayName("should return RoleResponse when name exists")
        void shouldReturnRole_whenNameExists() {
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            assertThat(roleService.getRoleByName("ADMIN").name()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when name not found")
        void shouldThrow_whenNameNotFound() {
            when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getRoleByName("UNKNOWN"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("name");
        }
    }

    // -------------------------------------------------------------------------
    // getAllRoles
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getAllRoles()")
    class GetAllRoles {

        @Test
        @DisplayName("should return paginated role list")
        void shouldReturnPaginatedRoles() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Role> rolePage = new PageImpl<>(List.of(role), pageable, 1);

            when(roleRepository.findAll(pageable)).thenReturn(rolePage);
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            Page<RoleResponse> result = roleService.getAllRoles(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("ADMIN");
        }
    }

    // -------------------------------------------------------------------------
    // updateRole
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateRole()")
    class UpdateRole {

        @Test
        @DisplayName("should update name when it changes and is unique")
        void shouldUpdateName_whenUnique() {
            UpdateRoleRequest request = new UpdateRoleRequest("SUPERADMIN");

            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(roleRepository.existsByName("SUPERADMIN")).thenReturn(false);
            when(roleRepository.save(role)).thenReturn(role);
            when(roleMapper.toResponse(role)).thenReturn(
                    new RoleResponse(1L, "SUPERADMIN", 1L, null, null));

            assertThat(roleService.updateRole(1L, request).name()).isEqualTo("SUPERADMIN");
        }

        @Test
        @DisplayName("should not call save when name is unchanged")
        void shouldSkipUpdate_whenNameUnchanged() {
            UpdateRoleRequest request = new UpdateRoleRequest("ADMIN");

            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(roleRepository.save(role)).thenReturn(role);
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            roleService.updateRole(1L, request);

            verify(roleRepository, never()).existsByName(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when new name is taken")
        void shouldThrow_whenNewNameExists() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(roleRepository.existsByName("USER")).thenReturn(true);

            assertThatThrownBy(() -> roleService.updateRole(1L, new UpdateRoleRequest("USER")))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.updateRole(99L, new UpdateRoleRequest("USER")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // deleteRole
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteRole()")
    class DeleteRole {

        @Test
        @DisplayName("should delete role when it exists")
        void shouldDelete_whenExists() {
            when(roleRepository.existsById(1L)).thenReturn(true);

            roleService.deleteRole(1L);

            verify(roleRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenNotFound() {
            when(roleRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> roleService.deleteRole(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role");

            verify(roleRepository, never()).deleteById(any());
        }
    }
}
