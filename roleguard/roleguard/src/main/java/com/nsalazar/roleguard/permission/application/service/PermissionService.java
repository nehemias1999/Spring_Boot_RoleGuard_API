package com.nsalazar.roleguard.permission.application.service;

import com.nsalazar.roleguard.permission.application.dto.CreatePermissionRequest;
import com.nsalazar.roleguard.permission.application.dto.PermissionResponse;
import com.nsalazar.roleguard.permission.application.dto.UpdatePermissionRequest;
import com.nsalazar.roleguard.permission.application.mapper.IPermissionMapper;
import com.nsalazar.roleguard.permission.domain.model.Permission;
import com.nsalazar.roleguard.permission.domain.port.in.IPermissionUseCase;
import com.nsalazar.roleguard.permission.domain.port.out.IPermissionRepositoryPort;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service implementing all Permission use cases.
 * Never exposes JPA entities outside this layer — all outputs are DTOs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService implements IPermissionUseCase {

    private final IPermissionRepositoryPort permissionRepository;
    private final IPermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        log.debug("Creating permission with name='{}'", request.name());

        validateNameUniqueness(request.name());

        Permission permission = permissionMapper.toEntity(request);
        Permission saved = permissionRepository.save(permission);

        log.info("Permission created — id={}, name='{}'", saved.getId(), saved.getName());
        return permissionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(UUID id) {
        log.debug("Fetching permission id={}", id);
        return permissionMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionByName(String name) {
        log.debug("Fetching permission by name='{}'", name);
        Permission permission = permissionRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "name", name));
        return permissionMapper.toResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionResponse> getAllPermissions(Pageable pageable) {
        log.debug("Fetching all permissions — page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return permissionRepository.findAll(pageable).map(permissionMapper::toResponse);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(UUID id, UpdatePermissionRequest request) {
        log.debug("Updating permission id={}", id);

        Permission permission = findOrThrow(id);

        if (request.name() != null && !request.name().equals(permission.getName())) {
            validateNameUniqueness(request.name());
            permission.setName(request.name());
        }

        Permission updated = permissionRepository.save(permission);
        log.info("Permission updated — id={}, name='{}'", updated.getId(), updated.getName());
        return permissionMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deletePermission(UUID id) {
        log.debug("Deleting permission id={}", id);
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission", "id", id);
        }
        permissionRepository.deleteById(id);
        log.info("Permission deleted — id={}", id);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Permission findOrThrow(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
    }

    private void validateNameUniqueness(String name) {
        if (permissionRepository.existsByName(name)) {
            throw new DuplicateResourceException("Permission", "name", name);
        }
    }
}
