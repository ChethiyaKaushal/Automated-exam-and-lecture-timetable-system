package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.LecturerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LecturerRepository extends JpaRepository<LecturerEntity, Long> {

    Optional<LecturerEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    List<LecturerEntity> findByDepartment_DepartmentId(Long departmentId);

    @EntityGraph(attributePaths = "modules")
    Optional<LecturerEntity> findWithModulesByLecturerId(Long lecturerId);
}
