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
@Table(name = "student_group")
public class StudentGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_size", nullable = false)
    private Integer groupSize;

    // StudentGroup -> Semester
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private SemesterEntity semester;

    // StudentGroup <-> Student
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_member",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<StudentEntity> students = new HashSet<>();

    // StudentGroup <-> Module
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "module_student_group",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "module_id")
    )
    private Set<ModuleEntity> modules = new HashSet<>();

    // StudentGroup -> StudentGroupAllocation
    @OneToMany(mappedBy = "studentGroup", fetch = FetchType.LAZY)
    private Set<StudentGroupAllocationEntity> allocations
            = new HashSet<>();
}