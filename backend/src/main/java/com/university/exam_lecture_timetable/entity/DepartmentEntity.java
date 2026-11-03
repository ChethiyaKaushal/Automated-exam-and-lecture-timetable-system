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
@Table(name = "department")
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "department_name", nullable = false)
    private String departmentName;

    // Head of department: a real relation instead of copied id + name
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hod_lecturer_id", unique = true)
    private LecturerEntity hod;

    @OneToMany(mappedBy = "department")
    private Set<StudentEntity> students = new HashSet<>();

    @OneToMany(mappedBy = "department")
    private Set<ModuleEntity> modules = new HashSet<>();

    @OneToMany(mappedBy = "department")
    private Set<LecturerEntity> lecturers = new HashSet<>();

    @Transient
    public String getHodName() {
        return hod != null ? hod.getLecturerName() : null;
    }

    @Transient
    public Long getHodLecturerId() {
        return hod != null ? hod.getLecturerId() : null;
    }
}
