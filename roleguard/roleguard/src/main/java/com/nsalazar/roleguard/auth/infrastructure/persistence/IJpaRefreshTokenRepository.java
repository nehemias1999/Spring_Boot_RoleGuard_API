package com.nsalazar.roleguard.auth.infrastructure.persistence;

import com.nsalazar.roleguard.auth.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RefreshToken}.
 * Acts as the technical implementation behind {@link com.nsalazar.roleguard.auth.domain.port.out.IRefreshTokenRepositoryPort}.
 */
public interface IJpaRefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Transactional
    void deleteByToken(String token);

    @Transactional
    void deleteByUsername(String username);
}
