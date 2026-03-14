package com.nsalazar.roleguard.auth.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persisted refresh token issued after successful login or registration.
 * <p>
 * One refresh token per user is enforced: creating a new token for a username
 * first deletes any existing token for that user (handled by {@link com.nsalazar.roleguard.auth.application.service.RefreshTokenService}).
 * </p>
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_token", columnList = "token"),
                @Index(name = "idx_refresh_tokens_username", columnList = "username")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Opaque UUID token value sent to the client. */
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    /** Username (subject) this token belongs to. */
    @Column(nullable = false, length = 50)
    private String username;

    /** Absolute expiry time; compared against current time on validation. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
