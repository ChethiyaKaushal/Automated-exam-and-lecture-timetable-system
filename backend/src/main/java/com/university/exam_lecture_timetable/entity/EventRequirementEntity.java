package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "event_requirement")
public class EventRequirementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requirement_id")
    private Long requirementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleEntity module;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 20)
    private RequirementType requirementType = RequirementType.LECTURE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private CalendarPeriodEntity period;

    @OneToOne(
            mappedBy = "requirement",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private LectureRequirementEntity lectureRequirement;

    @OneToOne(
            mappedBy = "requirement",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private ExamRequirementEntity examRequirement;
}