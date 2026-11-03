package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.ExamRequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRequirementRepository extends JpaRepository<ExamRequirementEntity, Long> {
}
