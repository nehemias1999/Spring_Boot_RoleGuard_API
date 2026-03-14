package com.nsalazar.roleguard.permission.infrastructure.adapter.out;

import com.nsalazar.roleguard.permission.domain.model.Permission;
import com.nsalazar.roleguard.permission.domain.port.out.IPermissionRepositoryPort;
import com.nsalazar.roleguard.permission.infrastructure.persistence.IJpaPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link IPermissionRepositoryPort}.
 * Delegates all operations to {@link IJpaPermissionRepository}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements IPermissionRepositoryPort {

    private final IJpaPermissionRepository jpaPermissionRepository;

    @Override
    public Permission save(Permission permission) {
        log.trace("Persisting permission id={}, name='{}'", permission.getId(), permission.getName());
        return jpaPermissionRepository.save(permission);
    }

    @Override
    public Optional<Permission> findById(UUID id) {
        log.trace("Querying permission by id={}", id);
        return jpaPermissionRepository.findById(id);
    }

    @Override
    public Optional<Permission> findByName(String name) {
        log.trace("Querying permission by name='{}'", name);
        return jpaPermissionRepository.findByName(name);
    }

    @Override
    public Page<Permission> findAll(Pageable pageable) {
        log.trace("Querying all permissions — page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return jpaPermissionRepository.findAll(pageable);
    }

    @Override
    public boolean existsById(UUID id) {
        log.trace("Checking existence of permission id={}", id);
        return jpaPermissionRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        log.trace("Checking existence of permission name='{}'", name);
        return jpaPermissionRepository.existsByName(name);
    }

    @Override
    public void deleteById(UUID id) {
        log.trace("Deleting permission id={}", id);
        jpaPermissionRepository.deleteById(id);
    }
}
