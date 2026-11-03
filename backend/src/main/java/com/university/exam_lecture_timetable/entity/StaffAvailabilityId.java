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
public class StaffAvailabilityId implements Serializable {

    @Column(name = "lecturer_id")
    private Long lecturerId;

    @Column(name = "timeslot_id")
    private Long timeslotId;
}
