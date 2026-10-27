package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "event_lecturer")
public class EventLecturerEntity {

    @EmbeddedId
    private EventLecturerId id = new EventLecturerId();

    @MapsId("eventId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "event_id",
            updatable = false)
    private EventEntity event;

    @MapsId("lecturerId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "lecturer_id",
            updatable = false)
    private LecturerEntity lecturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private EventAllocationEntity allocation;

    @Column(name = "role")
    private String role;
}
