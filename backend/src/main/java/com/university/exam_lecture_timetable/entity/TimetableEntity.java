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

    @Column(name = "version_num", nullable = false)
    private Integer versionNum = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TimetableStatus status = TimetableStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "timetable_type", nullable = false, length = 20)
    private RequirementType timetableType = RequirementType.LECTURE;

    @Column(name = "published_at")
    private Instant publishedAt;

    // Timetable -> CalendarPeriod
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private CalendarPeriodEntity period;

    // Timetable -> Department
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    // Timetable -> Creator
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUserEntity createdByUser;

    // Timetable -> Publisher
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private AppUserEntity publishedByUser;

    // Timetable -> Events
    @OneToMany(mappedBy = "timetable", fetch = FetchType.LAZY)
    private Set<EventEntity> events = new HashSet<>();
}