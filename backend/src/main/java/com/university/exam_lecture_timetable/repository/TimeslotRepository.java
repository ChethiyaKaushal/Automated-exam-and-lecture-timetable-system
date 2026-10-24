package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.TimeslotEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeslotRepository extends JpaRepository<TimeslotEntity, Long> {

    List<TimeslotEntity> findByPeriodIdOrderByDateAscTimeslotAsc(Long periodId);
    boolean existsByPeriodIdAndDateAndTimeslot(Long periodId, LocalDate date, String timeslot);
}
