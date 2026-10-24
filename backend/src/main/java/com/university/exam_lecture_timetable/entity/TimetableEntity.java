
package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "timetable",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timetable_period_department_version",
                        columnNames = {
                                "period_id",
                                "department_id",
                                "version_num"
                        }
                )
        }
)
public class TimetableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_id")
    private Long timetableId;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "version_num", nullable = false)
    private Integer versionNum = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TimetableStatus status = TimetableStatus.DRAFT;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    // Calendar Period Relationship
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "period_id",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private CalendarPeriodEntity period;

    // Department Relationship
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "department_id",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private DepartmentEntity department;

    // Timetable Creator
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            insertable = false,
            updatable = false
    )
    private AppUserEntity createdByUser;

    // Timetable Publisher
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "published_by",
            insertable = false,
            updatable = false
    )
    private AppUserEntity publishedByUser;

    // Events belonging to this timetable
    @OneToMany(mappedBy = "timetable")
    private Set<EventEntity> events = new HashSet<>();
}
