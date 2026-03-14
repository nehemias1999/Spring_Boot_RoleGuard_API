package com.nsalazar.roleguard.permission.infrastructure.persistence;

import com.nsalazar.roleguard.permission.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Permission}.
 * Acts as the technical implementation behind
 * {@link com.nsalazar.roleguard.permission.domain.port.out.IPermissionRepositoryPort}.
 */
public interface IJpaPermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);
}
