package com.nsalazar.roleguard.permission.domain.model;

import com.nsalazar.roleguard.role.domain.model.Role;
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
 * Permission entity representing a fine-grained action that can be granted to one or more roles.
 * <p>
 * Key design decisions:
 * <ul>
 *   <li>Many-to-many with {@link Role}: a permission can belong to many roles and a role can
 *       have many permissions. {@link Role} owns the join table {@code role_permissions}.</li>
 *   <li>{@code @Version} enables optimistic locking on concurrent updates.</li>
 *   <li>Name convention: uppercase with underscores (e.g. "READ_USERS", "DELETE_ROLES").</li>
 * </ul>
 * </p>
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "permissions",
        indexes = {
                @Index(name = "idx_permissions_name", columnList = "name")
        }
)
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique permission name. Convention: uppercase with underscores (e.g. "READ_USERS"). */
    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false, length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 50)
    private String updatedBy;

    /**
     * Inverse side of the Role-Permission many-to-many relationship.
     * Role is the owning side — changes must be made via Role.permissions.
     */
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission permission)) return false;
        return id != null && id.equals(permission.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
