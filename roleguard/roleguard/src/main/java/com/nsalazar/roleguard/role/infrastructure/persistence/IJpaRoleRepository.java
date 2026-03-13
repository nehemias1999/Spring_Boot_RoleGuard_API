package com.nsalazar.roleguard.role.infrastructure.persistence;

import com.nsalazar.roleguard.role.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Role}.
 * Acts as the technical implementation behind {@link com.nsalazar.roleguard.role.domain.port.out.IRoleRepositoryPort}.
 * Paginated {@code findAll(Pageable)} is inherited from {@link JpaRepository}.
 */
public interface IJpaRoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}
