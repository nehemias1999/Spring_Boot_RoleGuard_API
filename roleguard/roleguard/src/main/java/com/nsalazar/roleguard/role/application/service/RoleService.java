package com.nsalazar.roleguard.role.application.service;

import com.nsalazar.roleguard.permission.domain.model.Permission;
import com.nsalazar.roleguard.permission.domain.port.out.IPermissionRepositoryPort;
import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.application.dto.UpdateRoleRequest;
import com.nsalazar.roleguard.role.application.mapper.IRoleMapper;
import com.nsalazar.roleguard.role.domain.model.Role;
import com.nsalazar.roleguard.role.domain.port.in.IRoleUseCase;
import com.nsalazar.roleguard.role.domain.port.out.IRoleRepositoryPort;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.ResourceInUseException;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import com.nsalazar.roleguard.user.domain.port.out.IUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service implementing all Role use cases.
 * Orchestrates domain rules and persistence.
 * Never exposes JPA entities outside this layer — all outputs are DTOs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService implements IRoleUseCase {

    private final IRoleRepositoryPort roleRepository;
    private final IPermissionRepositoryPort permissionRepository;
    private final IUserRepositoryPort userRepository;
    private final IRoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        log.debug("Creating role with name='{}'", request.name());

        validateNameUniqueness(request.name());

        Role role = roleMapper.toEntity(request);
        Role saved = roleRepository.save(role);

        log.info("Role created — id={}, name='{}'", saved.getId(), saved.getName());
        return roleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
        log.debug("Fetching role id={}", id);
        return roleMapper.toResponse(findRoleOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleByName(String name) {
        log.debug("Fetching role by name='{}'", name);
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", name));
        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> getAllRoles(Pageable pageable) {
        log.debug("Fetching all roles — page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return roleRepository.findAll(pageable).map(roleMapper::toResponse);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
        log.debug("Updating role id={}", id);

        Role role = findRoleOrThrow(id);

        if (request.name() != null && !request.name().equals(role.getName())) {
            validateNameUniqueness(request.name());
            role.setName(request.name());
        }

        Role updated = roleRepository.save(role);
        log.info("Role updated — id={}, name='{}'", updated.getId(), updated.getName());
        return roleMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        log.debug("Deleting role id={}", id);
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role", "id", id);
        }
        long userCount = userRepository.countByRoleId(id);
        if (userCount > 0) {
            throw new ResourceInUseException("Role", "user(s)", userCount);
        }
        roleRepository.deleteById(id);
        log.info("Role deleted — id={}", id);
    }

    @Override
    @Transactional
    public RoleResponse assignPermission(UUID roleId, UUID permissionId) {
        log.debug("Assigning permission id={} to role id={}", permissionId, roleId);

        Role role = findRoleOrThrow(roleId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));

        role.getPermissions().add(permission);
        Role updated = roleRepository.save(role);

        log.info("Permission id={} assigned to role id={}", permissionId, roleId);
        return roleMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public RoleResponse removePermission(UUID roleId, UUID permissionId) {
        log.debug("Removing permission id={} from role id={}", permissionId, roleId);

        Role role = findRoleOrThrow(roleId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));

        role.getPermissions().remove(permission);
        Role updated = roleRepository.save(role);

        log.info("Permission id={} removed from role id={}", permissionId, roleId);
        return roleMapper.toResponse(updated);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Role findRoleOrThrow(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
    }

    private void validateNameUniqueness(String name) {
        if (roleRepository.existsByName(name)) {
            throw new DuplicateResourceException("Role", "name", name);
        }
    }
}
