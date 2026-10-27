package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name="event_slot")
@Getter
@Setter
@NoArgsConstructor
public class EventSlotEntity {

    @EmbeddedId
    private EventSlotId id = new EventSlotId();

    @MapsId("eventId")
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="event_id", nullable=false)
    private EventEntity event;

    @MapsId("timeslotId")
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="timeslot_id", nullable=false)
    private TimeslotEntity timeslot;
}
