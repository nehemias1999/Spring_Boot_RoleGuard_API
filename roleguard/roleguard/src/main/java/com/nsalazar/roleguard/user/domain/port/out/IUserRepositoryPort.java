package com.nsalazar.roleguard.user.domain.port.out;

import com.nsalazar.roleguard.user.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port defining the persistence contract for User.
 * The application layer depends on this interface — never on JPA or Spring Data directly.
 */
public interface IUserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Page<User> findAll(Pageable pageable);

    boolean existsById(UUID id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRoleId(UUID roleId);

    void deleteById(UUID id);
}
