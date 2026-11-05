package com.university.exam_lecture_timetable.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveRequirementsRequest(
        @NotEmpty List<@NotNull Long> periodIds,
        @NotEmpty List<@NotNull @Valid ModuleRequirement> modules
) {
    public record ModuleRequirement(
            @NotNull Long moduleId,
            @NotNull Boolean hasExam,
            Integer examDurationMinutes
    ) {}
}
