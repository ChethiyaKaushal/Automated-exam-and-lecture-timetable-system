package com.university.exam_lecture_timetable.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ModuleFilterRequestDto(
        @NotEmpty
        List<Long> semesterIds
) {
}
