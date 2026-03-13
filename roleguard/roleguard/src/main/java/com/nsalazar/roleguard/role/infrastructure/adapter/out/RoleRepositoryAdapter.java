package com.nsalazar.roleguard.role.infrastructure.adapter.out;

import com.nsalazar.roleguard.role.domain.model.Role;
import com.nsalazar.roleguard.role.domain.port.out.IRoleRepositoryPort;
import com.nsalazar.roleguard.role.infrastructure.persistence.IJpaRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistence adapter implementing {@link IRoleRepositoryPort}.
 * Delegates all operations to {@link IJpaRoleRepository}, isolating the domain from JPA details.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements IRoleRepositoryPort {

    private final IJpaRoleRepository jpaRoleRepository;

    @Override
    public Role save(Role role) {
        log.trace("Persisting role id={}, name='{}'", role.getId(), role.getName());
        return jpaRoleRepository.save(role);
    }

    @Override
    public Optional<Role> findById(Long id) {
        log.trace("Querying role by id={}", id);
        return jpaRoleRepository.findById(id);
    }

    @Override
    public Optional<Role> findByName(String name) {
        log.trace("Querying role by name='{}'", name);
        return jpaRoleRepository.findByName(name);
    }

    @Override
    public Page<Role> findAll(Pageable pageable) {
        log.trace("Querying all roles — page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return jpaRoleRepository.findAll(pageable);
    }

    @Override
    public boolean existsById(Long id) {
        log.trace("Checking existence of role id={}", id);
        return jpaRoleRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        log.trace("Checking existence of role name='{}'", name);
        return jpaRoleRepository.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        log.trace("Deleting role id={}", id);
        jpaRoleRepository.deleteById(id);
    }
}
