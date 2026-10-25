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
        name = "event_allocation",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"event_id", "venue_id"}
                )
        }
)

public class EventAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "venue_id",  nullable = false)
    private Long venueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id",
            insertable = false,
            updatable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id",
            insertable = false,
            updatable = false)
    private VenueEntity venue;


    @OneToMany(
            mappedBy = "eventAllocation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<StudentGroupAllocationEntity> studentGroups
            = new HashSet<>();
}
