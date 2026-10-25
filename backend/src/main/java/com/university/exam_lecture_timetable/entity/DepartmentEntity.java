package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "department")
public class DepartmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "department_name", nullable = false)
    private String departmentName;

    @Column(name = "hod_lecturer_id")
    private Long hodLecturerId;

    @Column(name = "hod_name")
    private String hodName;

    @OneToMany(mappedBy = "department")
    private Set<StudentEntity> students = new HashSet<>();

    @OneToMany(mappedBy = "department")
    private Set<ModuleEntity> modules = new HashSet<>();

    @OneToMany(mappedBy = "department")
    private Set<LecturerEntity> lecturers = new HashSet<>();

}
