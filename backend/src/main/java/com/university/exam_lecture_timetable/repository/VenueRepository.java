package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.VenueEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepository extends JpaRepository<VenueEntity, Long> {

    List<VenueEntity> findByVenueType(String venueType);
    List<VenueEntity> findByIsAvailableForExamsTrue();
    List<VenueEntity> findByLectureCapacityGreaterThanEqual(Integer capacity);
}
