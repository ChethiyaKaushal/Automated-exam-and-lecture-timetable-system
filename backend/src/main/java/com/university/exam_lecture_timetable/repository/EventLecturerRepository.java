package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EventLecturerEntity;
import com.university.exam_lecture_timetable.entity.EventLecturerId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLecturerRepository extends JpaRepository<EventLecturerEntity, EventLecturerId> {

    List<EventLecturerEntity> findByIdEventId(Long eventId);
    List<EventLecturerEntity> findByIdLecturerId(Long lecturerId);

    @Modifying
    @Query("delete from EventLecturerEntity e where e.id.eventId = :eventId")
    void deleteByIdEventId(@Param("eventId") Long eventId);
}
