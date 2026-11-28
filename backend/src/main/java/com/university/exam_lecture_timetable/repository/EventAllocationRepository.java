package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EventAllocationEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.university.exam_lecture_timetable.entity.TimetableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventAllocationRepository extends JpaRepository<EventAllocationEntity, Long> {

    List<EventAllocationEntity> findByEvent_EventId(Long eventId);
    List<EventAllocationEntity> findByVenue_VenueId(Long venueId);
    boolean existsByEvent_EventIdAndVenue_VenueId(Long eventId, Long venueId);

    /**
     * Venue/timeslot pairs in this period that are already booked by saved events,
     * ignoring ARCHIVED timetables and the requirements that are being scheduled now
     * (those will be replaced by the new solver result).
     */
    @Query("""
            select distinct a.venue.venueId as venueId, s.timeslot.timeslotId as timeslotId
            from EventAllocationEntity a
            join a.event e
            join e.eventSlots s
            join e.timetable t
            where s.timeslot.period.periodId = :periodId
                and t.status <> :ignoredStatus
                and e.requirement.requirementId not in :scheduledRequirementIds
            """)
    List<BookedSlot> findBookedSlots(
            @Param("periodId") Long periodId,
            @Param("scheduledRequirementIds") Collection<Long> scheduledRequirementIds,
            @Param("ignoredStatus") TimetableStatus ignoredStatus);

    interface BookedSlot {
        Long getVenueId();
        Long getTimeslotId();
    }
}
