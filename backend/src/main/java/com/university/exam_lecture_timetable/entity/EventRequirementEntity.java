package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.*;
import org.hibernate.annotations.ColumnDefault;

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

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LECTURE'")
    @Column(name = "requirement_type", nullable = false, length = 20)
    private RequirementType requirementType;

    @Column(name = "period_id")
    private Long periodId;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "module_id",
            insertable = false,
            updatable = false)
    private ModuleEntity module;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "period_id",
            insertable = false,
            updatable = false)
    private CalendarPeriodEntity period;


    @OneToOne(mappedBy = "requirement", cascade = CascadeType.ALL, orphanRemoval = true)
    private LectureRequirementEntity lectureRequirement;

    @OneToOne(mappedBy = "requirement", cascade = CascadeType.ALL, orphanRemoval = true)
    private ExamRequirementEntity examRequirement;

}
