
package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EventSlotEntity;
import com.university.exam_lecture_timetable.entity.EventSlotId;
import com.university.exam_lecture_timetable.entity.RequirementType;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSlotRepository
        extends JpaRepository<EventSlotEntity, EventSlotId> {

    List<EventSlotEntity> findByIdEventId(Long eventId);

    List<EventSlotEntity> findByIdTimeslotId(Long timeslotId);

    @Modifying
    @Query("""
        DELETE FROM EventSlotEntity e
        WHERE e.id.eventId = :eventId
    """)
    void deleteByIdEventId(
            @Param("eventId") Long eventId
    );

    @Query("""
        SELECT es FROM EventSlotEntity es
        JOIN FETCH es.event e
        JOIN FETCH e.requirement r
        JOIN FETCH r.module
        JOIN FETCH e.timetable t
        JOIN FETCH es.timeslot
        WHERE t.timetableId = :timetableId
          AND r.requirementType = :requirementType
    """)
    List<EventSlotEntity> findExamSlots(
            @Param("timetableId") Long timetableId,
            @Param("requirementType") RequirementType requirementType
    );
}
