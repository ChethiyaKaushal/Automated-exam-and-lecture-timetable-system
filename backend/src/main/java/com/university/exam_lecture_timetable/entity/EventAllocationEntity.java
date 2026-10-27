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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    @OneToMany(
            mappedBy = "eventAllocation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<StudentGroupAllocationEntity> studentGroups
            = new HashSet<>();
}