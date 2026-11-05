package com.university.exam_lecture_timetable.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record StartAssignment(
        Long requirementId, Long startTimeslotId,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<Long> venueIds
) {
    public StartAssignment(Long requirementId, Long startTimeslotId) {
        this(requirementId, startTimeslotId, null);
    }
}
