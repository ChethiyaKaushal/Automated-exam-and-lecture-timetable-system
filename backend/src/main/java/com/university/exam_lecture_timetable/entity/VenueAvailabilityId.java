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
public class VenueAvailabilityId implements Serializable {

    @Column(name = "venue_id")
    private Long venueId;

    @Column(name = "timeslot_id")
    private Long timeslotId;

}
