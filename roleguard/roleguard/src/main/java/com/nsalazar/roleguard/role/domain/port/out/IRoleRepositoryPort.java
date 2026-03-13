package com.nsalazar.roleguard.role.domain.port.out;

import com.nsalazar.roleguard.role.domain.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Output port defining the persistence contract for Role.
 * The application layer depends on this interface — never on JPA or Spring Data directly.
 */
public interface IRoleRepositoryPort {

    Role save(Role role);

    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);

    Page<Role> findAll(Pageable pageable);

    boolean existsById(Long id);

    boolean existsByName(String name);

    void deleteById(Long id);
}
