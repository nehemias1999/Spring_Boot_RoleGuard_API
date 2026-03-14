package com.nsalazar.roleguard.auth.infrastructure.adapter.out;

import com.nsalazar.roleguard.auth.domain.model.RefreshToken;
import com.nsalazar.roleguard.auth.domain.port.out.IRefreshTokenRepositoryPort;
import com.nsalazar.roleguard.auth.infrastructure.persistence.IJpaRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistence adapter implementing {@link IRefreshTokenRepositoryPort}.
 * Delegates all operations to {@link IJpaRefreshTokenRepository}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements IRefreshTokenRepositoryPort {

    private final IJpaRefreshTokenRepository jpaRefreshTokenRepository;

    @Override
    public RefreshToken save(RefreshToken token) {
        log.trace("Persisting refresh token for username='{}'", token.getUsername());
        return jpaRefreshTokenRepository.save(token);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        log.trace("Querying refresh token");
        return jpaRefreshTokenRepository.findByToken(token);
    }

    @Override
    public void deleteByToken(String token) {
        log.trace("Deleting refresh token by value");
        jpaRefreshTokenRepository.deleteByToken(token);
    }

    @Override
    public void deleteByUsername(String username) {
        log.trace("Deleting refresh token for username='{}'", username);
        jpaRefreshTokenRepository.deleteByUsername(username);
    }
}
