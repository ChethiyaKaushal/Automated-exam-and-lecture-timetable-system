package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventSlotId implements Serializable {

    @Column(name="event_id")
    private Long eventId;

    @Column(name="timeslot_id")
    private Long timeslotId;

}
