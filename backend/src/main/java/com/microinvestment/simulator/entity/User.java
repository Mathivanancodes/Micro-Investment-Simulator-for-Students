package com.microinvestment.simulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Locale;

@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    protected User() { }

    public User(String name, String email, String passwordHash) {
        // INTERVIEW: this(...) constructor chaining centralizes JPA initialization before business validation.
        this();
        this.name = EntityRules.text(name, 100, "Name");
        this.email = EntityRules.text(email, 254, "Email").toLowerCase(Locale.ROOT);
        this.passwordHash = EntityRules.text(passwordHash, 100, "Password hash");
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
