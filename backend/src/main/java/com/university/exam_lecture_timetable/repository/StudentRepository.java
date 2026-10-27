package com.university.exam_lecture_timetable.repository;

import java.util.List;
import java.util.Optional;

import com.university.exam_lecture_timetable.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    Optional<StudentEntity> findByRegistrationNum(String registrationNum);
    boolean existsByRegistrationNum(String registrationNum);
    List<StudentEntity> findByDepartment_DepartmentId(Long departmentId);
}
