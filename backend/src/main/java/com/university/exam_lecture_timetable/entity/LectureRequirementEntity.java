package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Subtype of Event_Requirement (1:1) - only for requirement_type = LECTURE
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lecture_requirement")
public class LectureRequirementEntity {

    @Id
    @Column(name = "requirement_id")
    private Long requirementId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "requirement_id")
    private EventRequirementEntity requirement;

}
