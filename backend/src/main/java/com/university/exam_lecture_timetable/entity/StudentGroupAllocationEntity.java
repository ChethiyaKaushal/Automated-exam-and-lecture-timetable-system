package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="student_group_allocation")
@Getter
@Setter
@NoArgsConstructor
public class StudentGroupAllocationEntity {

    @EmbeddedId
    private StudentGroupAllocationId id = new StudentGroupAllocationId();

    @MapsId("allocationId")
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="allocation_id", nullable=false)
    private EventAllocationEntity eventAllocation;

    @MapsId("groupId")
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="group_id", nullable=false)
    private StudentGroupEntity studentGroup;

    @Column(name = "allocated_count", nullable = false)
    private Integer allocatedCount;
}
