package com.university.exam_lecture_timetable.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StudentGroupAllocationId implements Serializable {
    @Column(name="allocation_id")
    private Long allocationId;

    @Column(name="group_id")
    private Long groupId;
}
