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
@Table(name = "lecturer")
public class LecturerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecturer_id")
    private Long lecturerId;

    @Column(name = "lecturer_name",  nullable = false)
    private String lecturerName;

    @Column(name = "email",  nullable = false,  unique = true)
    private String email;

    @Column(name = "department_id",  nullable = false)
    private Long departmentId;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "max_weekly_hour")
    private Integer maxWeeklyHour;

    @ManyToOne(fetch = FetchType.LAZY,  optional = false)
    @JoinColumn(
            name = "department_id",
            insertable = false,
            updatable = false)
    private DepartmentEntity department;

    @ManyToMany
    @JoinTable(
            name = "lecturer_module",
            joinColumns = @JoinColumn(name = "lecturer_id"),
            inverseJoinColumns = @JoinColumn(name = "module_id"))
    private Set<ModuleEntity> modules = new HashSet<>();

    @OneToMany(mappedBy = "lecturer")
    private Set<StaffAvailabilityEntity> staffAvailabilities = new HashSet<>();

    // Event_Lecturer
    @OneToMany(mappedBy = "lecturer")
    private Set<EventLecturerEntity> eventLecturers = new HashSet<>();

}
