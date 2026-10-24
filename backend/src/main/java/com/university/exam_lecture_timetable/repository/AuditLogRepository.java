package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.AuditLogEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByUserIdOrderByOccurredAtDesc(Long userId);
    List<AuditLogEntity> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, String entityId);
    List<AuditLogEntity> findByOccurredAtBetween(Instant from, Instant to);
}
