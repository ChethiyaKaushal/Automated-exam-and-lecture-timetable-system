package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_entry_entity", columnList = "entry_type, entity_id"),
                @Index(name = "idx_audit_user", columnList = "user_id"),
                @Index(name = "idx_audit_occurred_at", columnList = "occurred_at")
        }
)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "entry_type", length = 100)
    private String entryType;

    // id of the record that was changed (e.g. the timetable_id)
    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_user_id")
    private UserEntity authUser;

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}