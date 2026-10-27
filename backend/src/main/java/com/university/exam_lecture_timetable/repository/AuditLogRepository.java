package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.AuditLogEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByUser_UserIdOrderByOccurredAtDesc(Long userId);
    List<AuditLogEntity> findByEntryTypeOrderByOccurredAtDesc(String entryType);
    List<AuditLogEntity> findByOccurredAtBetween(Instant from, Instant to);
}
