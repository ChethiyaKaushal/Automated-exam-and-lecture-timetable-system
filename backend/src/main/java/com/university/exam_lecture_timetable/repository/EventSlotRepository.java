package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EventSlotEntity;
import com.university.exam_lecture_timetable.entity.EventSlotId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSlotRepository extends JpaRepository<EventSlotEntity, EventSlotId> {

    List<EventSlotEntity> findByIdEventId(Long eventId);
    List<EventSlotEntity> findByIdTimeslotId(Long timeslotId);

    @Modifying
    @Query("delete from EventSlotEntity e where e.id.eventId = :eventId")
    void deleteByIdEventId(@Param("eventId") Long eventId);
}
