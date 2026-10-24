package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.StudentGroupEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroupEntity, Long> {

    List<StudentGroupEntity> findBySemesterId(Long semesterId);

    @Query("select g from StudentGroupEntity g join g.modules m where m.moduleId = :moduleId")
    List<StudentGroupEntity> findByModuleId(@Param("moduleId") Long moduleId);
}
