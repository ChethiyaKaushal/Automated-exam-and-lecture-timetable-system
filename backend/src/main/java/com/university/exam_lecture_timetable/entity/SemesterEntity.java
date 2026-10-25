package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "semester")
public class SemesterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "semester_name")
    private String semesterName;

    @Column(name = "starting_date", nullable = false)
    private LocalDate startingDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(mappedBy = "semester")
    private Set<CalendarPeriodEntity> calendarPeriods = new HashSet<>();

    @OneToMany(mappedBy = "semester")
    private Set<ModuleEntity> modules = new HashSet<>();

    @OneToMany(mappedBy = "semester")
    private Set<StudentGroupEntity> studentGroups = new HashSet<>();
}
