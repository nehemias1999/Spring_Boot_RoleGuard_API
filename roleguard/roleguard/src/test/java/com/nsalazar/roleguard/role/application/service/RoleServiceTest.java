package com.nsalazar.roleguard.role.application.service;

import com.nsalazar.roleguard.permission.domain.model.Permission;
import com.nsalazar.roleguard.permission.domain.port.out.IPermissionRepositoryPort;
import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.application.dto.UpdateRoleRequest;
import com.nsalazar.roleguard.role.application.mapper.IRoleMapper;
import com.nsalazar.roleguard.role.domain.model.Role;
import com.nsalazar.roleguard.role.domain.port.out.IRoleRepositoryPort;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.ResourceInUseException;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import com.nsalazar.roleguard.user.domain.port.out.IUserRepositoryPort;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService")
class RoleServiceTest {

    private static final UUID ROLE_ID    = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNKNOWN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Mock private IRoleRepositoryPort roleRepository;
    @Mock private IPermissionRepositoryPort permissionRepository;
    @Mock private IUserRepositoryPort userRepository;
    @Mock private IRoleMapper roleMapper;

    @InjectMocks private RoleService roleService;

    private Role role;
    private RoleResponse roleResponse;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(ROLE_ID)
                .name("ADMIN")
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        roleResponse = new RoleResponse(ROLE_ID, "ADMIN", 0L, role.getCreatedAt(), role.getUpdatedAt(), List.of());
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
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            assertThat(roleService.getRoleById(ROLE_ID).id()).isEqualTo(ROLE_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenNotFound() {
            when(roleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getRoleById(UNKNOWN_ID))
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

            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(roleRepository.existsByName("SUPERADMIN")).thenReturn(false);
            when(roleRepository.save(role)).thenReturn(role);
            when(roleMapper.toResponse(role)).thenReturn(
                    new RoleResponse(ROLE_ID, "SUPERADMIN", 1L, null, null, List.of()));

            assertThat(roleService.updateRole(ROLE_ID, request).name()).isEqualTo("SUPERADMIN");
        }

        @Test
        @DisplayName("should not call save when name is unchanged")
        void shouldSkipUpdate_whenNameUnchanged() {
            UpdateRoleRequest request = new UpdateRoleRequest("ADMIN");

            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(roleRepository.save(role)).thenReturn(role);
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            roleService.updateRole(ROLE_ID, request);

            verify(roleRepository, never()).existsByName(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when new name is taken")
        void shouldThrow_whenNewNameExists() {
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(roleRepository.existsByName("USER")).thenReturn(true);

            assertThatThrownBy(() -> roleService.updateRole(ROLE_ID, new UpdateRoleRequest("USER")))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenNotFound() {
            when(roleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.updateRole(UNKNOWN_ID, new UpdateRoleRequest("USER")))
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
        @DisplayName("should delete role when it exists and no users are assigned")
        void shouldDelete_whenExists() {
            when(roleRepository.existsById(ROLE_ID)).thenReturn(true);
            when(userRepository.countByRoleId(ROLE_ID)).thenReturn(0L);

            roleService.deleteRole(ROLE_ID);

            verify(roleRepository).deleteById(ROLE_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenNotFound() {
            when(roleRepository.existsById(UNKNOWN_ID)).thenReturn(false);

            assertThatThrownBy(() -> roleService.deleteRole(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role");

            verify(roleRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw ResourceInUseException when users are assigned to the role")
        void shouldThrow_whenUsersAssigned() {
            when(roleRepository.existsById(ROLE_ID)).thenReturn(true);
            when(userRepository.countByRoleId(ROLE_ID)).thenReturn(3L);

            assertThatThrownBy(() -> roleService.deleteRole(ROLE_ID))
                    .isInstanceOf(ResourceInUseException.class)
                    .hasMessageContaining("3");

            verify(roleRepository, never()).deleteById(any());
        }
    }

    // -------------------------------------------------------------------------
    // assignPermission
    // -------------------------------------------------------------------------

    private static final UUID PERMISSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Nested
    @DisplayName("assignPermission()")
    class AssignPermission {

        @Test
        @DisplayName("should add permission to role and return updated RoleResponse")
        void shouldAssignPermission_whenBothExist() {
            Permission permission = Permission.builder()
                    .id(PERMISSION_ID).name("READ_USERS").version(0L).build();

            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));
            when(roleRepository.save(role)).thenReturn(role);
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            RoleResponse result = roleService.assignPermission(ROLE_ID, PERMISSION_ID);

            assertThat(result).isNotNull();
            verify(roleRepository).save(role);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenRoleNotFound() {
            when(roleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.assignPermission(UNKNOWN_ID, PERMISSION_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when permission does not exist")
        void shouldThrow_whenPermissionNotFound() {
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(permissionRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.assignPermission(ROLE_ID, UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Permission");
        }
    }

    // -------------------------------------------------------------------------
    // removePermission
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("removePermission()")
    class RemovePermission {

        @Test
        @DisplayName("should remove permission from role and return updated RoleResponse")
        void shouldRemovePermission_whenBothExist() {
            Permission permission = Permission.builder()
                    .id(PERMISSION_ID).name("READ_USERS").version(0L).build();
            role.getPermissions().add(permission);

            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));
            when(roleRepository.save(role)).thenReturn(role);
            when(roleMapper.toResponse(role)).thenReturn(roleResponse);

            RoleResponse result = roleService.removePermission(ROLE_ID, PERMISSION_ID);

            assertThat(result).isNotNull();
            verify(roleRepository).save(role);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenRoleNotFound() {
            when(roleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.removePermission(UNKNOWN_ID, PERMISSION_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when permission does not exist")
        void shouldThrow_whenPermissionNotFound() {
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(permissionRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.removePermission(ROLE_ID, UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Permission");
        }
    }
}
