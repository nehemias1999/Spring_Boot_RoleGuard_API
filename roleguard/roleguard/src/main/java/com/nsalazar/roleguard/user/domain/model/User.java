package com.nsalazar.roleguard.user.domain.model;

import com.nsalazar.roleguard.role.domain.model.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core User entity.
 * <p>
 * Key design decisions:
 * <ul>
 *   <li>{@code password} and {@code role} are excluded from {@code toString()} —
 *       prevents the BCrypt hash from leaking into logs and avoids
 *       {@code LazyInitializationException} on the LAZY role proxy.</li>
 *   <li>{@code enabled} controls whether the user can authenticate; defaults to {@code true}.</li>
 *   <li>{@code @Version} enables optimistic locking — concurrent updates on the same
 *       row will throw {@code ObjectOptimisticLockingFailureException} instead of silently
 *       overwriting each other.</li>
 *   <li>Indexes on {@code username} and {@code email} ensure O(log n) lookups on the
 *       most frequent query paths.</li>
 *   <li>{@code createdBy} / {@code updatedBy} are populated by Spring Data JPA Auditing
 *       via {@link com.nsalazar.roleguard.shared.config.AuditorAwareImpl}.</li>
 * </ul>
 * </p>
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_email", columnList = "email")
        }
)
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /** BCrypt-hashed password. Excluded from toString to prevent hash exposure in logs. */
    @ToString.Exclude
    @Column(nullable = false)
    private String password;

    /**
     * Whether the user account is active. Defaults to {@code true}.
     * Set to {@code false} to disable the account without deleting it.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Optimistic locking version. Managed entirely by Hibernate — do not set manually.
     */
    @Version
    private Long version;

    /**
     * Role assigned to this user. LAZY fetch — access only within an active transaction.
     * Excluded from toString to prevent LazyInitializationException in log statements.
     */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    /** Automatically set by Hibernate on first persist. Never updated. */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Null on creation; set to current time on every UPDATE. */
    private LocalDateTime updatedAt;

    /** Username of the principal who created this record. Set once at insert. */
    @CreatedBy
    @Column(updatable = false, length = 50)
    private String createdBy;

    /** Username of the principal who last modified this record. */
    @LastModifiedBy
    @Column(length = 50)
    private String updatedBy;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
