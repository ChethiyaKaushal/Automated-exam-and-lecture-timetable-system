package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByTimetable_TimetableId(Long timetableId);
    List<EventEntity> findByRequirement_RequirementId(Long requirementId);
    List<EventEntity> findByTimetable_TimetableIdAndEventType(Long timetableId, String eventType);
}
