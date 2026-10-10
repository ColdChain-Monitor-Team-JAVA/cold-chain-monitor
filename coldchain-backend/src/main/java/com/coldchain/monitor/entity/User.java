package com.coldchain.monitor.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String name;
    @Column(nullable = false, unique = true) public String username;
    @Column(nullable = false, unique = true) public String email;
    @Column(nullable = false) public String password; // BCrypt hash, never serialized by APIs
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Role role;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Status status = Status.ACTIVE;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant lastLogin;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
