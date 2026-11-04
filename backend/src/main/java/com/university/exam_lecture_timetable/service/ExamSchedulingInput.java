package com.university.exam_lecture_timetable.service;

import java.util.List;

/** Scheduling data passed to the prompt. Student identities stay in the backend. */
public record ExamSchedulingInput(
        Long periodId,
        List<Exam> exams,
        List<Slot> timeslots,
        List<Venue> venues,
        List<Conflict> studentConflicts
) {
    public record Exam(
            Long requirementId,
            Long moduleId,
            String moduleCode,
            String moduleName,
            String curriculum,
            int durationMinutes,
            int studentCount,
            List<Group> groups
    ) {}

    public record Group(Long groupId, int studentCount) {}

    public record Slot(Long timeslotId, String date, String startTime, String endTime) {}

    public record Venue(Long venueId, String venueName, int examCapacity, List<Long> unavailableTimeslotIds) {}

    public record Conflict(Long firstRequirementId, Long secondRequirementId) {}
}
