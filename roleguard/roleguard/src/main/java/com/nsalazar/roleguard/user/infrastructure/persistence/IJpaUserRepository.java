package com.nsalazar.roleguard.user.infrastructure.persistence;

import com.nsalazar.roleguard.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User}.
 * Acts as the technical implementation behind {@link com.nsalazar.roleguard.user.domain.port.out.IUserRepositoryPort}.
 * Paginated {@code findAll(Pageable)} is inherited from {@link JpaRepository}.
 */
public interface IJpaUserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
