package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        })
public class AppUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "locked", nullable = false)
    private Boolean locked = false;

    @Column(name="student_id", unique=true)
    private Long studentId;

    @Column(name="lecturer_id", unique=true)
    private Long lecturerId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "student_id",
            insertable = false,
            updatable = false
    )
    private StudentEntity student;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "lecturer_id",
            insertable = false,
            updatable = false
    )
    private LecturerEntity lecturer;

    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private java.util.Set<RoleEntity> roles = new java.util.HashSet<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
