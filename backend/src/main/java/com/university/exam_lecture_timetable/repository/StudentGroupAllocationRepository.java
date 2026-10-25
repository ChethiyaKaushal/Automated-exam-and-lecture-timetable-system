package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.StudentGroupAllocationEntity;
import com.university.exam_lecture_timetable.entity.StudentGroupAllocationId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupAllocationRepository extends JpaRepository<StudentGroupAllocationEntity, StudentGroupAllocationId> {

    List<StudentGroupAllocationEntity> findByIdAllocationId(Long allocationId);
    List<StudentGroupAllocationEntity> findByIdGroupId(Long groupId);
}
