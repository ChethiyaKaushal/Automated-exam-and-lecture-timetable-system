package com.university.exam_lecture_timetable.service;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.university.exam_lecture_timetable.controller.AdminExamTimetableController;
import com.university.exam_lecture_timetable.controller.TimeTableSetupController;
import com.university.exam_lecture_timetable.dto.request.StartAssignment;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.Draft;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.RoomAllocation;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.ScheduledExam;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Review;
import com.university.exam_lecture_timetable.exception.GeminiProviderException;
import com.university.exam_lecture_timetable.exception.GlobalExceptionHandler;
import com.university.exam_lecture_timetable.validation.ExamTimetableReviewValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeminiAdminPreviewTests {

    private static final String PREVIEW = "/api/admin/exam-timetables/1/preview";
    private final ExamTimetableDraftService java = mock(ExamTimetableDraftService.class);
    private final GeminiExamTimetableService gemini = mock(GeminiExamTimetableService.class);
    private final ExamTimetableReviewValidator review = mock(ExamTimetableReviewValidator.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var admin = new AdminExamTimetableService(null, null, null, null, null, null, null, java, gemini, review);
        mvc = MockMvcBuilders.standaloneSetup(new AdminExamTimetableController(admin),
                        new TimeTableSetupController(mock(TimetableSetupService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void geminiPreviewCarriesSelectedVenuesWithoutCallingTheJavaEngine() throws Exception {
        var room = new RoomAllocation(9L, "Hall 9", "A-I", 60, 40, List.of());
        var exam = new ScheduledExam(5L, "AA1201", "Maths", "C-23", 120, 40,
                "2026-12-01", "09:00", "11:00", List.of(4L, 5L, 6L, 7L), List.of(room));
        var assignments = List.of(new StartAssignment(5L, 4L, List.of(9L)));
        when(gemini.generate(1L)).thenReturn(new Draft(1L, "DRAFT", false, true, List.of(exam), List.of()));
        when(review.review(1L, assignments)).thenReturn(new Review(true, false, List.of(), List.of(exam)));

        mvc.perform(post(PREVIEW).param("engine", "gemini"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].venueIds[0]").value(9))
                .andExpect(jsonPath("$.review.exams[0].venues[0].venueId").value(9))
                .andExpect(jsonPath("$.review.valid").value(true))
                .andExpect(jsonPath("$.review.studentMembershipsComplete").value(false));
        verify(gemini).generate(1L);
        verify(review).review(1L, assignments);
        verifyNoInteractions(java);
    }

    @Test
    void unavailableGeminiReturnsAnActionable503WithoutSilentlySwitchingEngines() throws Exception {
        when(gemini.generate(1L)).thenThrow(new GeminiProviderException(503, "GEMINI_UNAVAILABLE", "Try again or choose Java."));
        mvc.perform(post(PREVIEW).param("engine", "gemini"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.issues[0].code").value("GEMINI_UNAVAILABLE"))
                .andExpect(jsonPath("$.issues[0].description").value("Try again or choose Java."));
        verifyNoInteractions(java, review);
    }

    @Test
    void rejectedEngineKeepsTheExamReviewIssueFormat() throws Exception {
        mvc.perform(post(PREVIEW).param("engine", "unknown"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.issues[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.issues[0].code").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.issues[0].description").value("Choose java or gemini as the draft engine."));
        verifyNoInteractions(java, gemini, review);
    }

    @Test
    void missingSetupParameterKeepsTheSharedApiErrorFormat() throws Exception {
        mvc.perform(get("/api/timetablesetup/semesters"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required parameter 'academicYear'"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void existingFrontendRequestStillDefaultsToJava() throws Exception {
        when(java.generate(1L)).thenReturn(new Draft(1L, "DRAFT", false, true, List.of(), List.of()));
        when(review.review(1L, List.of())).thenReturn(new Review(true, false, List.of(), List.of()));
        mvc.perform(post(PREVIEW)).andExpect(status().isOk());
        verify(java).generate(1L);
        verifyNoInteractions(gemini);
    }
}
