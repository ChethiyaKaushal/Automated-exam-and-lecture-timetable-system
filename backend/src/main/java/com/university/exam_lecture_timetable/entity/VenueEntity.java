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
@Table(name = "venue")
public class VenueEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "venue_id")
    private Long venueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue_type", nullable = false)
    private VenueType venueType;

    @Column(name = "venue_name",   nullable = false)
    private String venueName;

    @Column(name = "lecture_capacity",   nullable = false)
    private Integer lectureCapacity;

    @Column(name = "exam_capacity")
    private Integer examCapacity;

    @Column(name = "priority",   nullable = false)
    private Integer priority;

    @Column(name = "is_available_for_exams", nullable = false)
    private Boolean isAvailableForExams = true;

    @OneToMany(mappedBy = "venue")
    private Set<VenueAvailabilityEntity> availabilities = new HashSet<>();

    @OneToMany(mappedBy = "venue")
    private Set<EventAllocationEntity> allocations = new HashSet<>();

}
