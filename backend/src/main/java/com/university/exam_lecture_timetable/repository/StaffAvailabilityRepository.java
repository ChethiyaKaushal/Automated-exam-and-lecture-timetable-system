package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.StaffAvailabilityEntity;
import com.university.exam_lecture_timetable.entity.StaffAvailabilityId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffAvailabilityRepository extends JpaRepository<StaffAvailabilityEntity, StaffAvailabilityId> {

    List<StaffAvailabilityEntity> findByIdLecturerId(Long lecturerId);
    List<StaffAvailabilityEntity> findByIdTimeslotId(Long timeslotId);
    List<StaffAvailabilityEntity> findByIdLecturerIdAndIsAvailableFalse(Long lecturerId);
}
