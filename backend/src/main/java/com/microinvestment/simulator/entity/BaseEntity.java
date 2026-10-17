package com.microinvestment.simulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

// INTERVIEW: A mapped abstract superclass shares identity and lifecycle behavior without creating a seventh table.
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BaseEntity() { }

    @PrePersist
    protected void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private static Class<?> persistentClass(Object entity) {
        return entity instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass() : entity.getClass();
    }

    private static Long identifier(BaseEntity entity) {
        return entity instanceof HibernateProxy proxy
                ? (Long) proxy.getHibernateLazyInitializer().getInternalIdentifier() : entity.getId();
    }

    // INTERVIEW: Override Object identity using persisted IDs; two unsaved objects remain distinct, including with JPA proxies.
    @Override
    public final boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || persistentClass(this) != persistentClass(other)) return false;
        Long identity = identifier(this);
        return identity != null && identity.equals(identifier((BaseEntity) other));
    }

    @Override
    public final int hashCode() {
        // A constant class hash stays stable when a transient object receives its generated ID.
        return persistentClass(this).hashCode();
    }

    @Override
    public final String toString() {
        // Final Object overrides bypass proxy interception, so even detached proxies need no database access.
        return persistentClass(this).getSimpleName() + "{id=" + identifier(this) + "}";
    }
}
