package com.nsalazar.roleguard.auth.application.service;

import com.nsalazar.roleguard.auth.domain.model.RefreshToken;
import com.nsalazar.roleguard.auth.domain.port.out.IRefreshTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manages the lifecycle of refresh tokens.
 * <p>
 * One refresh token per user is enforced: creating a new token for a username
 * deletes any previously issued token for that user.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final IRefreshTokenRepositoryPort refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    /**
     * Creates a new refresh token for the given username, revoking any existing token first.
     *
     * @param username the user to issue a token for
     * @return the persisted {@link RefreshToken}
     */
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        refreshTokenRepository.deleteByUsername(username);
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .username(username)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .build();
        RefreshToken saved = refreshTokenRepository.save(token);
        log.debug("Refresh token created for username='{}'", username);
        return saved;
    }

    /**
     * Validates a refresh token and returns it if valid.
     * Deletes the token and throws {@link BadCredentialsException} if expired.
     *
     * @param token the raw token string from the client
     * @return the matching {@link RefreshToken}
     * @throws BadCredentialsException if the token is not found or has expired
     */
    @Transactional
    public RefreshToken validateAndGet(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found");
                    return new BadCredentialsException("Invalid refresh token");
                });
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(token);
            log.warn("Expired refresh token for username='{}'", rt.getUsername());
            throw new BadCredentialsException("Refresh token expired");
        }
        return rt;
    }

    /**
     * Revokes all refresh tokens for the given username (logout).
     *
     * @param username the user to log out
     */
    @Transactional
    public void deleteByUsername(String username) {
        refreshTokenRepository.deleteByUsername(username);
        log.debug("Refresh token revoked for username='{}'", username);
    }
}
