package com.nsalazar.roleguard.role.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Role entity representing a permission level assignable to users (e.g. "USER", "ADMIN").
 * <p>
 * Key design decisions:
 * <ul>
 *   <li>{@code @Version} enables optimistic locking — concurrent updates throw
 *       {@code ObjectOptimisticLockingFailureException} instead of silently overwriting.</li>
 *   <li>Index on {@code name} ensures O(log n) lookups; name is the primary lookup key
 *       for Spring Security authority resolution.</li>
 *   <li>Deleting a role that has users assigned will be rejected by the FK constraint —
 *       handled as HTTP 409 via {@link com.nsalazar.roleguard.shared.exception.GlobalExceptionHandler}.</li>
 * </ul>
 * </p>
 */
@Entity
@Table(
        name = "roles",
        indexes = {
                @Index(name = "idx_roles_name", columnList = "name")
        }
)
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique role name. Convention: uppercase without "ROLE_" prefix (e.g. "ADMIN", "USER"). */
    @Column(unique = true, nullable = false, length = 50)
    private String name;

    /**
     * Optimistic locking version. Managed entirely by Hibernate — do not set manually.
     * Incremented on every UPDATE; a stale-read conflict throws
     * {@code ObjectOptimisticLockingFailureException}.
     */
    @Version
    private Long version;

    /** Automatically set by Hibernate on first persist. Never updated. */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Null on creation; set to current time on every UPDATE. */
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return id != null && id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
