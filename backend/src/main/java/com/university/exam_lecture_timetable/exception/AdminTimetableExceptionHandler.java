package com.university.exam_lecture_timetable.exception;

import com.university.exam_lecture_timetable.controller.AdminExamTimetableController;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Issue;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AdminExamTimetableController.class)
public class AdminTimetableExceptionHandler {

    @ExceptionHandler(GeminiProviderException.class)
    public ResponseEntity<Map<String, List<Issue>>> providerFailure(GeminiProviderException error) {
        return ResponseEntity.status(error.httpStatus()).body(Map.of("issues",
                List.of(new Issue("ERROR", error.code(), error.getMessage(), List.of()))));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public Map<String, List<Issue>> rejected(RuntimeException error) {
        return Map.of("issues", List.of(new Issue("ERROR", "REVIEW_REQUIRED", error.getMessage(), List.of())));
    }
}
