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
@Table(name = "event_lecturer")
public class EventLecturerEntity {

    @EmbeddedId
    private EventLecturerId id = new EventLecturerId();

    @MapsId("eventId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_id",
            updatable = false)
    private EventEntity event;

    @MapsId("lecturerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", updatable = false)
    private LecturerEntity lecturer;

    @Column(name = "allocation_id")
    private Long allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "allocation_id",
            insertable = false,
            updatable = false
    )
    private EventAllocationEntity allocation;

    @Column(name = "role")
    private String role;
}
