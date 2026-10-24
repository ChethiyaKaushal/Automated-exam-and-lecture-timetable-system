package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByTimetableId(Long timetableId);
    List<EventEntity> findByRequirementId(Long requirementId);
    List<EventEntity> findByTimetableIdAndEventType(Long timetableId, String eventType);
}
