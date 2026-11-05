package com.university.exam_lecture_timetable.dto.respose;

public record ModuleResponseDto(
        Long moduleId,
        String moduleCode,
        String moduleName,
        String curriculum,
        Long semesterId,
        String semesterName,
        Long departmentId,
        String departmentName
) {
}
