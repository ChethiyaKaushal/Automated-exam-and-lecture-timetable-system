package com.university.exam_lecture_timetable.dto.response;

import com.university.exam_lecture_timetable.dto.request.StartAssignment;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.ScheduledExam;
import java.time.LocalDate;
import java.util.List;

public final class ExamTimetableResponses {

    private ExamTimetableResponses() {}

    public record PeriodOption(Long periodId, String name, LocalDate start, LocalDate end) {}
    public record ExamOption(Long moduleId, String moduleCode, String moduleName, String curriculum, String semester,
                             boolean enabled, int durationMinutes) {}
    public record SlotOption(Long id, String date, String time) {}
    public record DraftOption(Long id, int version) {}
    public record Setup(PeriodOption period, List<ExamOption> exams, List<SlotOption> slots, List<DraftOption> drafts) {}
    public record Preview(List<StartAssignment> assignments, Review review) {}
    public record Saved(Long timetableId, Integer version, Review review, boolean manualReviewRequired) {}

    public record Issue(String severity, String code, String description, List<Long> requirementIds) {}
    public record Review(boolean valid, boolean studentMembershipsComplete, List<Issue> issues,
                         List<ScheduledExam> exams) {}
}
