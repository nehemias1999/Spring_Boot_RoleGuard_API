package com.nsalazar.roleguard.user.infrastructure.persistence;

import com.nsalazar.roleguard.user.domain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User}.
 * Acts as the technical implementation behind {@link com.nsalazar.roleguard.user.domain.port.out.IUserRepositoryPort}.
 * Paginated {@code findAll(Pageable)} is inherited from {@link JpaRepository}.
 */
public interface IJpaUserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Loads a user with its role eagerly — avoids LazyInitializationException
     * when reading role name outside a transaction (e.g. inside JwtAuthFilter).
     */
    long countByRoleId(UUID roleId);

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<User> findWithRoleByUsername(String username);
}
