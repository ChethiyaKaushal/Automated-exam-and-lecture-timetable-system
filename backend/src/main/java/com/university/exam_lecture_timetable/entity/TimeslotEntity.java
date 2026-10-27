package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "timeslot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timeslot_period_date_slot",
                        columnNames = {"period_id", "date", "timeslot"}
                )
        }
)
public class TimeslotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timeslot_id")
    private Long timeslotId;

    @Column(name = "timeslot", nullable = false)
    private String timeslot;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // Timeslot -> CalendarPeriod
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private CalendarPeriodEntity period;

    // Timeslot -> EventSlot
    @OneToMany(mappedBy = "timeslot", fetch = FetchType.LAZY)
    private Set<EventSlotEntity> eventSlots = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void validateTimes() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalStateException("endTime must be after startTime");
        }
    }
}