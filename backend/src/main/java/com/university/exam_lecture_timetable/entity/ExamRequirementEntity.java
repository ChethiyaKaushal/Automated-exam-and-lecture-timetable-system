package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Subtype of Event_Requirement (1:1) - only for requirement_type = EXAM
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "exam_requirement")
public class ExamRequirementEntity {

    @Id
    @Column(name = "requirement_id")
    private Long requirementId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "requirement_id")
    private EventRequirementEntity requirement;

    @Column(name = "has_exam", nullable = false)
    private Boolean hasExam = true;

    @Column(name = "exam_duration_minutes", nullable = false)
    private Integer examDurationMinutes;

}
