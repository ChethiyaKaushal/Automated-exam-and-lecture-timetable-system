package com.university.exam_lecture_timetable.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record CreateSetupRequestDto(
        @NotNull
        LocalDate startingDate,

        @NotBlank
        String academicYear,

        @NotNull
        LocalDate endingDate,

        @NotBlank
        String eventType,

        @NotEmpty
        List<Long> semesterIds
) {
}
