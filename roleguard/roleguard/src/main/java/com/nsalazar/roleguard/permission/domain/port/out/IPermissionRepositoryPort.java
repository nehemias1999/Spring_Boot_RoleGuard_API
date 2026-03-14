package com.nsalazar.roleguard.permission.domain.port.out;

import com.nsalazar.roleguard.permission.domain.model.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port defining the persistence contract for Permission.
 * The application layer depends on this interface — never on JPA or Spring Data directly.
 */
public interface IPermissionRepositoryPort {

    Permission save(Permission permission);

    Optional<Permission> findById(UUID id);

    Optional<Permission> findByName(String name);

    Page<Permission> findAll(Pageable pageable);

    boolean existsById(UUID id);

    boolean existsByName(String name);

    void deleteById(UUID id);
}
