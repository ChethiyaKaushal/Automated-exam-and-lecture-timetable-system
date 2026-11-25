package com.university.exam_lecture_timetable.dto.request;

import com.university.exam_lecture_timetable.entity.VenueType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveRequirementsRequest(
        @NotEmpty List<@NotNull Long> periodIds,
        @NotEmpty List<@NotNull @Valid ModuleRequirement> modules,
        @Valid List<@NotNull SharedExamPairRequest> sharedExamPairs
) {
    public record ModuleRequirement(
            @NotNull Long moduleId,
            @NotNull Boolean hasExam,
            Integer examDurationMinutes,
            Integer examGapDays,
            VenueType requiredVenueType
    ) {}

    public record SharedExamPairRequest(
            @NotEmpty
            List<@NotNull Long> moduleIds
    ) {}
}
