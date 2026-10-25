package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.DepartmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    boolean existsByDepartmentNameIgnoreCase(String departmentName);
    Optional<DepartmentEntity> findByDepartmentNameIgnoreCase(String departmentName);
}
