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
@Table(name = "calendar_period")
public class CalendarPeriodEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "period_id")
    private Long periodId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "period_type")
    private String periodType;

    @Column(name = "period_name")
    private String periodName;

    @Column(name = "starting_date")
    private LocalDate startingDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", insertable = false, updatable = false)
    private SemesterEntity semester;

    @OneToMany(mappedBy = "period")
    private Set<TimeslotEntity> timeslots = new HashSet<>();

    @OneToMany(mappedBy = "period")
    private Set<TimetableEntity> timetables = new HashSet<>();

}
