package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.*;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "event_requirement")
public class EventRequirementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requirement_id")
    private Long requirementId;

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Column(name = "duration_minutes" , nullable = false)
    private Integer durationMinutes;

    @Column(name = "required_venue_type")
    private String requiredVenueType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "module_id",
            insertable = false,
            updatable = false)
    private ModuleEntity module;

}
