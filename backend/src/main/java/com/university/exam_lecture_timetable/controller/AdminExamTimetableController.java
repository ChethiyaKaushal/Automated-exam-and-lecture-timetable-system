package com.university.exam_lecture_timetable.controller;

import com.university.exam_lecture_timetable.dto.request.DraftRequest;
import com.university.exam_lecture_timetable.dto.request.ExamSettings;
import com.university.exam_lecture_timetable.dto.request.StartAssignment;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.PeriodOption;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Preview;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Review;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Saved;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Setup;
import com.university.exam_lecture_timetable.exception.ExamTimetableReviewException;
import com.university.exam_lecture_timetable.service.AdminExamTimetableService;
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
    public List<PeriodOption> periods() {
        return timetables.periods();
    }

    @GetMapping("/{periodId}")
    public Setup setup(@PathVariable long periodId) {
        return timetables.setup(periodId);
    }

    @PutMapping("/{periodId}/exams/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void configure(@PathVariable long periodId, @PathVariable long moduleId, @RequestBody ExamSettings settings) {
        if (settings.enabled() == null || settings.durationMinutes() == null) {
            throw new ExamTimetableReviewException("Choose whether the exam is enabled and its duration.");
        }
        timetables.configure(periodId, moduleId, settings.enabled(), settings.durationMinutes());
    }

    @PostMapping("/{periodId}/preview")
    public Preview preview(
            @PathVariable long periodId, @RequestParam(defaultValue = "java") String engine
    ) {
        return timetables.preview(periodId, engine);
    }

    @GetMapping("/{periodId}/drafts/{timetableId}")
    public Preview open(@PathVariable long periodId, @PathVariable long timetableId) {
        return timetables.open(periodId, timetableId);
    }

    @PostMapping("/{periodId}/review")
    public Review review(@PathVariable long periodId, @RequestBody DraftRequest request) {
        return timetables.review(periodId, assignments(request));
    }

    @PostMapping("/{periodId}/drafts")
    public ResponseEntity<Saved> save(
            @PathVariable long periodId, @RequestBody DraftRequest request, Principal principal
    ) {
        var result = timetables.save(periodId, assignments(request),
                Boolean.TRUE.equals(request.manualStudentReview()), principal.getName());
        return ResponseEntity.status(result.timetableId() == null ? 422 : 201).body(result);
    }

    private static List<StartAssignment> assignments(DraftRequest request) {
        if (request.assignments() == null) {
            throw new ExamTimetableReviewException("Send the proposed exam start times.");
        }
        return request.assignments();
    }
}
