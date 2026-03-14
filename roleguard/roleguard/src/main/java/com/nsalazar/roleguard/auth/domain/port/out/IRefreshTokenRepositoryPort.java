package com.nsalazar.roleguard.auth.domain.port.out;

import com.nsalazar.roleguard.auth.domain.model.RefreshToken;

import java.util.Optional;

/**
 * Output port defining the persistence contract for {@link RefreshToken}.
 * The application layer depends on this interface — never on JPA or Spring Data directly.
 */
public interface IRefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUsername(String username);
}
