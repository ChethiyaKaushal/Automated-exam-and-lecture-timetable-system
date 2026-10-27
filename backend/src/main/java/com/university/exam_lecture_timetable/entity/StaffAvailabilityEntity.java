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
@Table(name = "staff_availability")
public class StaffAvailabilityEntity {

    @EmbeddedId
    private StaffAvailabilityId id = new StaffAvailabilityId();

    @MapsId("lecturerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", updatable = false)
    private LecturerEntity lecturer;

    @MapsId("timeslotId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timeslot_id", updatable = false)
    private TimeslotEntity timeslot;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    public StaffAvailabilityEntity(LecturerEntity lecturer, TimeslotEntity timeslot, Boolean isAvailable ) {
        this.id = new StaffAvailabilityId(lecturer.getLecturerId(), timeslot.getTimeslotId());
        this.lecturer = lecturer;
        this.timeslot = timeslot;
        this.isAvailable = isAvailable;
    }

}
