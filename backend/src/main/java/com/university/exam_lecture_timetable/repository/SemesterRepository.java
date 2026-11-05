package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.SemesterEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterRepository extends JpaRepository<SemesterEntity, Long> {

    List<SemesterEntity> findByAcademicYear(String academicYear);

    @Query("select s from SemesterEntity s where :date between s.startingDate and s.endDate")
    Optional<SemesterEntity> findCurrent(@Param("date") LocalDate date);

    @Query("""
    SELECT DISTINCT s.academicYear
    FROM SemesterEntity s
    WHERE s.academicYear IS NOT NULL
    ORDER BY s.academicYear
    """)
    List<String> findDistinctAcademicYears();

    List<SemesterEntity> findByAcademicYearOrderBySemesterNameAsc(
            String academicYear
    );
}
