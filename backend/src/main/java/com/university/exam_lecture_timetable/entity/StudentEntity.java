package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student")
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "registration_num", nullable = false, unique = true)
    private String registrationNum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id",
            insertable = false,
            updatable = false)
    private DepartmentEntity department;

    @ManyToMany(mappedBy = "students")
    private Set<StudentGroupEntity> groups = new HashSet<>();
}
