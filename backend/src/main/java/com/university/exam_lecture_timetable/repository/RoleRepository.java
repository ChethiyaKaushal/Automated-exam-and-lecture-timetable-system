package com.university.exam_lecture_timetable.repository;

import java.util.List;
import java.util.Optional;

import com.university.exam_lecture_timetable.entity.RoleEntity;
import com.university.exam_lecture_timetable.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(RoleName name);
    boolean existsByName(RoleName name);
}
