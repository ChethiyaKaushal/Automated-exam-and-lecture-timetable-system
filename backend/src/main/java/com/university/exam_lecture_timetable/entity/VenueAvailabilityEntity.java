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
@Table(name = "venue_availability")
public class VenueAvailabilityEntity {

    @EmbeddedId
    private VenueAvailabilityId id = new VenueAvailabilityId();

    @MapsId("venueId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id")
    private VenueEntity venue;

    @MapsId("timeslotId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timeslot_id")
    private TimeslotEntity timeslot;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    public VenueAvailabilityEntity(VenueEntity venue, TimeslotEntity timeslot, Boolean isAvailable) {
        this.id = new VenueAvailabilityId(venue.getVenueId(), timeslot.getTimeslotId());
        this.venue = venue;
        this.timeslot = timeslot;
        this.isAvailable = isAvailable;
    }
}
