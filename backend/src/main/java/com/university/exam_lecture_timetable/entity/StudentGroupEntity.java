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
@Table(name = "student_group")
public class StudentGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "group_size", nullable = false)
    private Integer groupSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id",
            insertable = false,
            updatable = false)
    private SemesterEntity semester;

    @ManyToMany
    @JoinTable(
            name = "group_member",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    private java.util.Set<StudentEntity> students = new java.util.HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "module_student_group",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "module_id"))
    private Set<ModuleEntity> modules = new HashSet<>();

    @OneToMany(mappedBy = "studentGroup")
    private Set<StudentGroupAllocationEntity> allocations
            = new HashSet<>();
}

