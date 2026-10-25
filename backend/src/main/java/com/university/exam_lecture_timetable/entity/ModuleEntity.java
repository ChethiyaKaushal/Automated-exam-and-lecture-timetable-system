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
@Table(name = "module")
public class ModuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "module_id")
    private Long moduleId;

    @Column(name = "module_code",  nullable = false)
    private String moduleCode;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "credit", nullable = false)
    private Integer credit;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "curriculum", nullable = false)
    private String curriculum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "department_id",
            insertable = false,
            updatable = false)
    private DepartmentEntity department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id",
            insertable = false,
            updatable = false)
    private SemesterEntity semester;

    // Lecturer_Module junction
    @ManyToMany(mappedBy = "modules")
    private Set<LecturerEntity> lecturers = new HashSet<>();

    @ManyToMany(mappedBy = "modules")
    private Set<StudentGroupEntity> studentGroups = new HashSet<>();

    @OneToMany(mappedBy = "module")
    private Set<EventRequirementEntity> eventRequirements = new HashSet<>();
}

