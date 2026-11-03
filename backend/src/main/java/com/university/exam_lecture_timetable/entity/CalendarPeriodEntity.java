package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "calendar_period")
public class CalendarPeriodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "period_id")
    private Long periodId;

    @Column(name = "period_type")
    private String periodType;

    @Column(name = "period_name")
    private String periodName;

    @Column(name = "starting_date")
    private LocalDate startingDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "calendar_period_semester",
            joinColumns = @JoinColumn(name = "period_id"),
            inverseJoinColumns = @JoinColumn(name = "semester_id")
    )
    private Set<SemesterEntity> semesters = new HashSet<>();

    @OneToMany(mappedBy = "period")
    private Set<TimeslotEntity> timeslots = new HashSet<>();

    @OneToMany(mappedBy = "period")
    private Set<TimetableEntity> timetables = new HashSet<>();
}