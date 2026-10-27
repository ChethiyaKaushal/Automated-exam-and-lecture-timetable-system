package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

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

    @Column(name = "module_code", nullable = false)
    private String moduleCode;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "credit", nullable = false)
    private Integer credit;

    @Column(name = "curriculum", nullable = false)
    private String curriculum;

    // Module -> Department
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    // Module -> Semester
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private SemesterEntity semester;

    // Module <-> Lecturer
    @ManyToMany(mappedBy = "modules", fetch = FetchType.LAZY)
    private Set<LecturerEntity> lecturers = new HashSet<>();

    // Module <-> StudentGroup
    @ManyToMany(mappedBy = "modules", fetch = FetchType.LAZY)
    private Set<StudentGroupEntity> studentGroups = new HashSet<>();

    // Module -> EventRequirement
    @OneToMany(mappedBy = "module", fetch = FetchType.LAZY)
    private Set<EventRequirementEntity> eventRequirements = new HashSet<>();
}