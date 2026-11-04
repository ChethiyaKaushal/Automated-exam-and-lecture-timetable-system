package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.ExamRequirementEntity;
import com.university.exam_lecture_timetable.entity.RequirementType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRequirementRepository extends JpaRepository<ExamRequirementEntity, Long> {

    @Query("""
            select exam
            from ExamRequirementEntity exam
            join fetch exam.requirement requirement
            join fetch requirement.module
            where requirement.period.periodId = :periodId
                and requirement.requirementType = :type
                and exam.hasExam = true
            order by exam.requirementId
            """)
    List<ExamRequirementEntity> findActiveByPeriodAndType(
            @Param("periodId") Long periodId, @Param("type") RequirementType type);
}
