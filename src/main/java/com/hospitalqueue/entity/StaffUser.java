package com.hospitalqueue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_user")
public class StaffUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StaffRole role;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    protected StaffUser() {
        // JPA
    }

    public StaffUser(String username, String passwordHash, StaffRole role, Doctor doctor) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.doctor = doctor;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public StaffRole getRole() {
        return role;
    }

    public Doctor getDoctor() {
        return doctor;
    }
}
