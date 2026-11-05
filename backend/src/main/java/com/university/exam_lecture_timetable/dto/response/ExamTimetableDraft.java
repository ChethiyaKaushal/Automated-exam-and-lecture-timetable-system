package com.university.exam_lecture_timetable.dto.response;

import java.util.List;

public final class ExamTimetableDraft {

    private ExamTimetableDraft() {}

    public record Draft(
            Long periodId, String status, boolean studentMembershipsComplete, boolean allExamsPlaced,
            List<ScheduledExam> exams, List<UnscheduledExam> unscheduledExams
    ) {}

    public record ScheduledExam(
            Long requirementId, String moduleCode, String moduleName, String curriculum,
            int durationMinutes, int studentCount, String date, String startTime, String endTime,
            List<Long> timeslotIds, List<RoomAllocation> venues
    ) {}

    public record RoomAllocation(
            Long venueId, String venueName, String allocationLabel, int examCapacity, int allocatedCount,
            List<GroupAllocation> groups
    ) {}

    public record GroupAllocation(Long groupId, int allocatedCount) {}

    public record UnscheduledExam(Long requirementId, String moduleCode, String moduleName, String curriculum,
                                 String reason) {}
}
