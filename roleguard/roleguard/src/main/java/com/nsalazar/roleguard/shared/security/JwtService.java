package com.nsalazar.roleguard.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Central service for JWT lifecycle management.
 * <p>
 * Handles token generation and validation using HMAC-SHA256 via {@code jjwt 0.12.x}.
 * The signing key is derived from a Base64-encoded secret injected via {@code jwt.secret}.
 * </p>
 * <p>
 * Key design decisions:
 * <ul>
 *   <li>The {@code role} claim is omitted entirely when the user has no role assigned,
 *       rather than storing an explicit {@code null}, to keep tokens minimal.</li>
 *   <li>{@link #isTokenValid} catches all {@link JwtException} subtypes and returns
 *       {@code false} instead of propagating them — callers only care about the boolean outcome.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        log.debug("JwtService initialised — expiration={}ms", expirationMs);
    }

    /**
     * Generates a signed JWT for the given subject.
     * <p>
     * Includes a {@code role} claim only when {@code roleName} is non-null.
     * </p>
     *
     * @param username subject to embed in the {@code sub} claim
     * @param roleName role name to embed as the {@code role} claim, or {@code null} to omit it
     * @return compact, URL-safe JWT string
     */
    public String generateToken(String username, String roleName) {
        log.debug("Generating JWT for username='{}', role='{}'", username, roleName);
        Map<String, Object> claims = new HashMap<>();
        if (roleName != null) {
            claims.put("role", roleName);
        }
        long now = System.currentTimeMillis();
        String token = Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
        log.debug("JWT generated for username='{}'", username);
        return token;
    }

    /**
     * Extracts the {@code sub} (username) claim from a token without validating its expiry.
     *
     * @param token compact JWT string
     * @return subject claim value
     * @throws JwtException if the token is malformed or the signature is invalid
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the {@code role} claim from a token without validating its expiry.
     *
     * @param token compact JWT string
     * @return role name, or {@code null} if the claim is absent
     * @throws JwtException if the token is malformed or the signature is invalid
     */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Validates a token against the expected username.
     * <p>
     * Returns {@code false} — rather than throwing — for any JWT error
     * (expired, malformed, tampered signature, etc.).
     * </p>
     *
     * @param token    compact JWT string to validate
     * @param username expected subject; must match the token's {@code sub} claim
     * @return {@code true} if the token is well-formed, not expired, and the subject matches
     */
    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            boolean valid = claims.getSubject().equals(username)
                    && !claims.getExpiration().before(new Date());
            if (!valid) {
                log.warn("JWT validation failed for username='{}' — subject mismatch or expired", username);
            }
            return valid;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
