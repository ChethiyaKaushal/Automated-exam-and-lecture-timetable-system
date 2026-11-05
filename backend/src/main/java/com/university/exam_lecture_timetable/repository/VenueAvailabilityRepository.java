package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.VenueAvailabilityEntity;
import com.university.exam_lecture_timetable.entity.VenueAvailabilityId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueAvailabilityRepository extends JpaRepository<VenueAvailabilityEntity, VenueAvailabilityId> {

    List<VenueAvailabilityEntity> findByIdVenueId(Long venueId);
    List<VenueAvailabilityEntity> findByIdTimeslotId(Long timeslotId);
    List<VenueAvailabilityEntity> findByIdVenueIdAndIsAvailableFalse(Long venueId);

    List<VenueAvailabilityEntity> findByTimeslot_Period_PeriodIdAndIsAvailableFalse(Long periodId);
}
