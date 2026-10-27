package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EventAllocationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventAllocationRepository extends JpaRepository<EventAllocationEntity, Long> {

    List<EventAllocationEntity> findByEvent_EventId(Long eventId);
    List<EventAllocationEntity> findByVenue_VenueId(Long venueId);
    boolean existsByEvent_EventIdAndVenue_VenueId(Long eventId, Long venueId);
}
