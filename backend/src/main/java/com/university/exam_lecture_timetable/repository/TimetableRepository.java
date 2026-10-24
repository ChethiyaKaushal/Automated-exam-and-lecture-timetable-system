package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.TimetableEntity;
import com.university.exam_lecture_timetable.entity.TimetableStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntity, Long> {

    List<TimetableEntity> findByPeriodIdAndDepartmentId(Long periodId, Long departmentId);
    Optional<TimetableEntity> findByPeriodIdAndDepartmentIdAndVersionNum(Long periodId, Long departmentId, Integer versionNum);
    List<TimetableEntity> findByStatus(TimetableStatus status);
    Optional<TimetableEntity> findFirstByPeriodIdAndDepartmentIdOrderByVersionNumDesc(Long periodId, Long departmentId);
}
