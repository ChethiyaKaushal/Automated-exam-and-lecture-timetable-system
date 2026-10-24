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
@Table(
        name = "timeslot",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"period_id", "date", "timeslot"}
        )
)
public class TimeslotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timeslot_id")
    private Long timeslotId;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "timeslot")
    private String timeslot;

    @Column(name = "date",  nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY,  optional = false)
    @JoinColumn(name = "period_id", insertable = false, updatable = false)
    private CalendarPeriodEntity period;

    @OneToMany(mappedBy = "timeslot")
    private Set<EventSlotEntity> eventSlots = new HashSet<>();
}



