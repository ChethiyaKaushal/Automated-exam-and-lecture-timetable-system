package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EventRequirementEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRequirementRepository extends JpaRepository<EventRequirementEntity, Long> {

    List<EventRequirementEntity> findByModule_ModuleId(Long moduleId);
}
