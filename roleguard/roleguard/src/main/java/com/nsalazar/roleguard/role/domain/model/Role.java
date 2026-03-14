package com.nsalazar.roleguard.role.domain.model;

import com.nsalazar.roleguard.permission.domain.model.Permission;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
 *   <li>{@code createdBy} / {@code updatedBy} are populated by Spring Data JPA Auditing
 *       via {@link com.nsalazar.roleguard.shared.config.AuditorAwareImpl}.</li>
 * </ul>
 * </p>
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique role name. Convention: uppercase without "ROLE_" prefix (e.g. "ADMIN", "USER"). */
    @Column(unique = true, nullable = false, length = 50)
    private String name;

    /**
     * Optimistic locking version. Managed entirely by Hibernate — do not set manually.
     */
    @Version
    private Long version;

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

    /**
     * Permissions granted to this role.
     * Role is the owning side — changes to this collection are persisted.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<Permission> permissions = new HashSet<>();

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
