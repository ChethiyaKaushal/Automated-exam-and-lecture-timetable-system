package com.university.exam_lecture_timetable.repository;

import java.util.List;
import java.util.Optional;

import com.university.exam_lecture_timetable.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends JpaRepository<ModuleEntity, Long> {

    Optional<ModuleEntity> findByModuleCode(String moduleCode);
    boolean existsByModuleCode(String moduleCode);
    List<ModuleEntity> findByDepartmentId(Long departmentId);
    List<ModuleEntity> findBySemesterId(Long semesterId);
    List<ModuleEntity> findByDepartmentIdAndSemesterId(Long departmentId, Long semesterId);
}
