package com.university.exam_lecture_timetable.repository;

import com.university.exam_lecture_timetable.entity.EnrollmentType;
import com.university.exam_lecture_timetable.entity.StudentModuleEnrollmentEntity;
import com.university.exam_lecture_timetable.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface StudentModuleEnrollmentRepository extends JpaRepository<StudentModuleEnrollmentEntity, Long> {
    // Only modules belonging to the timetable's chosen examination scope.
    // This detects regular-regular, regular-repeat and repeat-repeat clashes.
    @Query("""
        select distinct a.module.moduleId, b.module.moduleId
        from StudentModuleEnrollmentEntity a, StudentModuleEnrollmentEntity b
        where a.student.studentId = b.student.studentId
          and a.module.moduleId < b.module.moduleId
          and a.registrationStatus = :status
          and b.registrationStatus = :status
          and a.semester.semesterId in :semesterIds
          and b.semester.semesterId in :semesterIds
          and a.module.moduleId in :moduleIds
          and b.module.moduleId in :moduleIds
    """)
    List<Object[]> findConflictingModulePairs(
        @Param("semesterIds") Collection<Long> semesterIds,
        @Param("moduleIds") Collection<Long> moduleIds,
        @Param("status") RegistrationStatus status);


    // One row per enrollment, used by ExamConflictService to build conflict pairs.
    //This is for exam type is proper or repeat
    @Query("""
        select e.student.studentId as studentId, e.module.moduleId as moduleId,
            e.semester.semesterId as semesterId, e.enrollmentType as enrollmentType
        from StudentModuleEnrollmentEntity e
        where e.module.moduleId in :moduleIds
          and e.registrationStatus = :status
    """)
    List<EnrollmentRow> findEnrollmentsForModules(
            @Param("moduleIds") Collection<Long> moduleIds,
            @Param("status") RegistrationStatus status);

    //Only we want column
    interface EnrollmentRow {
        Long getStudentId();
        Long getModuleId();
        Long getSemesterId();
        EnrollmentType getEnrollmentType();
    }
}
