package com.university.exam_lecture_timetable.controller;

import com.university.exam_lecture_timetable.service.AdminExamTimetableService;
import com.university.exam_lecture_timetable.service.GeminiExamTimetableService.ProviderException;
import com.university.exam_lecture_timetable.service.ExamTimetableReviewService.Issue;
import com.university.exam_lecture_timetable.service.ExamTimetableReviewService.StartAssignment;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/exam-timetables")
public class AdminExamTimetableController {

    private final AdminExamTimetableService timetables;

    public AdminExamTimetableController(AdminExamTimetableService timetables) {
        this.timetables = timetables;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("header", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/periods")
    public List<AdminExamTimetableService.PeriodOption> periods() {
        return timetables.periods();
    }

    @GetMapping("/{periodId}")
    public AdminExamTimetableService.Setup setup(@PathVariable long periodId) {
        return timetables.setup(periodId);
    }

    @PutMapping("/{periodId}/exams/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void configure(@PathVariable long periodId, @PathVariable long moduleId, @RequestBody ExamSettings settings) {
        if (settings.enabled() == null || settings.durationMinutes() == null) {
            throw new IllegalArgumentException("Choose whether the exam is enabled and its duration.");
        }
        timetables.configure(periodId, moduleId, settings.enabled(), settings.durationMinutes());
    }

    @PostMapping("/{periodId}/preview")
    public AdminExamTimetableService.Preview preview(
            @PathVariable long periodId, @RequestParam(defaultValue = "java") String engine
    ) {
        return timetables.preview(periodId, engine);
    }

    @GetMapping("/{periodId}/drafts/{timetableId}")
    public AdminExamTimetableService.Preview open(@PathVariable long periodId, @PathVariable long timetableId) {
        return timetables.open(periodId, timetableId);
    }

    @PostMapping("/{periodId}/review")
    public com.university.exam_lecture_timetable.service.ExamTimetableReviewService.Review review(@PathVariable long periodId, @RequestBody DraftRequest request) {
        return timetables.review(periodId, assignments(request));
    }

    @PostMapping("/{periodId}/drafts")
    public ResponseEntity<AdminExamTimetableService.Saved> save(
            @PathVariable long periodId, @RequestBody DraftRequest request, Principal principal
    ) {
        var result = timetables.save(periodId, assignments(request),
                Boolean.TRUE.equals(request.manualStudentReview()), principal.getName());
        return ResponseEntity.status(result.timetableId() == null ? 422 : 201).body(result);
    }

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<Map<String, List<Issue>>> providerFailure(ProviderException error) {
        return ResponseEntity.status(error.httpStatus()).body(Map.of("issues",
                List.of(new Issue("ERROR", error.code(), error.getMessage(), List.of()))));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public Map<String, List<Issue>> rejected(RuntimeException error) {
        return Map.of("issues", List.of(new Issue("ERROR", "REVIEW_REQUIRED", error.getMessage(), List.of())));
    }

    private static List<StartAssignment> assignments(DraftRequest request) {
        if (request.assignments() == null) {
            throw new IllegalArgumentException("Send the proposed exam start times.");
        }
        return request.assignments();
    }

    public record ExamSettings(Boolean enabled, Integer durationMinutes) {}
    public record DraftRequest(List<StartAssignment> assignments, Boolean manualStudentReview) {}
}
