package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.PasswordResetTokenEntity;
import com.university.exam_lecture_timetable.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetTokenEntity p SET p.used = true WHERE p.user = :user AND p.used = false")
    void markAllUnusedTokensAsUsedForUser(@Param("user") UserEntity user);

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity p WHERE p.expiresAt < :now OR p.used = true")
    void deleteExpiredOrUsedTokens(@Param("now") LocalDateTime now);
}
