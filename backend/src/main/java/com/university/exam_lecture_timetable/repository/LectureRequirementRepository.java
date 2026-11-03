package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.LectureRequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LectureRequirementRepository extends JpaRepository<LectureRequirementEntity, Long> {
}
