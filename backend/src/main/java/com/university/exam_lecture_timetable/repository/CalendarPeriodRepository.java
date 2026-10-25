package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.CalendarPeriodEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarPeriodRepository extends JpaRepository<CalendarPeriodEntity, Long> {

    List<CalendarPeriodEntity> findBySemesterId(Long semesterId);
    List<CalendarPeriodEntity> findBySemesterIdAndPeriodType(Long semesterId, String periodType);
}
