package com.nsalazar.roleguard.user.infrastructure.adapter.out;

import com.nsalazar.roleguard.user.domain.model.User;
import com.nsalazar.roleguard.user.domain.port.out.IUserRepositoryPort;
import com.nsalazar.roleguard.user.infrastructure.persistence.IJpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link IUserRepositoryPort}.
 * Delegates all operations to {@link IJpaUserRepository}, isolating the domain from JPA details.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements IUserRepositoryPort {

    private final IJpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        log.trace("Persisting user id={}, username='{}'", user.getId(), user.getUsername());
        return jpaUserRepository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        log.trace("Querying user by id={}", id);
        return jpaUserRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        log.trace("Querying user by username='{}'", username);
        return jpaUserRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.trace("Querying user by email='{}'", email);
        return jpaUserRepository.findByEmail(email);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        log.trace("Querying all users — page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return jpaUserRepository.findAll(pageable);
    }

    @Override
    public boolean existsById(UUID id) {
        log.trace("Checking existence of user id={}", id);
        return jpaUserRepository.existsById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        log.trace("Checking existence of username='{}'", username);
        return jpaUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        log.trace("Checking existence of email='{}'", email);
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public long countByRoleId(UUID roleId) {
        log.trace("Counting users with role id={}", roleId);
        return jpaUserRepository.countByRoleId(roleId);
    }

    @Override
    public void deleteById(UUID id) {
        log.trace("Deleting user id={}", id);
        jpaUserRepository.deleteById(id);
    }
}
