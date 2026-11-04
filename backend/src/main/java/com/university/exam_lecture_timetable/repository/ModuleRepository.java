package com.university.exam_lecture_timetable.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.university.exam_lecture_timetable.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends JpaRepository<ModuleEntity, Long> {

    Optional<ModuleEntity> findByModuleCode(String moduleCode);
    boolean existsByModuleCode(String moduleCode);
    List<ModuleEntity> findByDepartment_DepartmentId(Long departmentId);
    List<ModuleEntity> findBySemester_SemesterId(Long semesterId);
    List<ModuleEntity> findByDepartment_DepartmentIdAndSemester_SemesterId(Long departmentId, Long semesterId);

    // Retrieve modules according to selected semesters
    @Query("""
    SELECT m
    FROM ModuleEntity m
    JOIN FETCH m.semester s
    JOIN FETCH m.department d
    WHERE s.semesterId IN :semesterIds
    ORDER BY d.departmentName ASC,
             s.semesterName ASC,
             m.moduleCode ASC
    """)
    List<ModuleEntity> findByScope(
            @Param("semesterIds") Collection<Long> semesterIds
    );

    List<ModuleEntity> findByModuleIdIn(
            Collection<Long> moduleIds
    );
}
