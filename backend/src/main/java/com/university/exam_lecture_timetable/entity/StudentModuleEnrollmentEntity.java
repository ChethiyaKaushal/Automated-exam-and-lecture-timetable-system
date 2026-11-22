package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_module_enrollment", uniqueConstraints =
    @UniqueConstraint(name = "uk_enrollment_student_module_semester",
            columnNames = {"student_id", "module_id", "semester_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StudentModuleEnrollmentEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleEntity module;

    // Semester in which the student registers this module (including repeat).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private SemesterEntity semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_type", nullable = false, length = 20)
    private EnrollmentType enrollmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus = RegistrationStatus.CONFIRMED;
}
