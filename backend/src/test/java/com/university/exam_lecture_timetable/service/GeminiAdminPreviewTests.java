package com.university.exam_lecture_timetable.service;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.university.exam_lecture_timetable.controller.AdminExamTimetableController;
import com.university.exam_lecture_timetable.service.ExamTimetableDraftService.Draft;
import com.university.exam_lecture_timetable.service.ExamTimetableReviewService.Review;
import com.university.exam_lecture_timetable.service.GeminiExamTimetableService.ProviderException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeminiAdminPreviewTests {

    private static final String PREVIEW = "/api/admin/exam-timetables/1/preview";
    private final ExamTimetableDraftService java = mock(ExamTimetableDraftService.class);
    private final GeminiExamTimetableService gemini = mock(GeminiExamTimetableService.class);
    private final ExamTimetableReviewService review = mock(ExamTimetableReviewService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var admin = new AdminExamTimetableService(null, null, null, null, null, null, null, java, gemini, review);
        mvc = MockMvcBuilders.standaloneSetup(new AdminExamTimetableController(admin)).build();
    }

    @Test
    void geminiPreviewReturnsTheSameReviewShapeWithoutUsingTheJavaEngineDirectly() throws Exception {
        when(gemini.generate(1L)).thenReturn(new Draft(1L, "DRAFT", false, true, List.of(), List.of()));
        when(review.review(1L, List.of())).thenReturn(new Review(true, false, List.of(), List.of()));

        mvc.perform(post(PREVIEW).param("engine", "gemini"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments").isArray())
                .andExpect(jsonPath("$.review.valid").value(true))
                .andExpect(jsonPath("$.review.studentMembershipsComplete").value(false));
        verify(gemini).generate(1L);
        verify(review).review(1L, List.of());
        verifyNoInteractions(java);
    }

    @Test
    void unavailableGeminiReturnsAnActionable503WithoutSilentlySwitchingEngines() throws Exception {
        when(gemini.generate(1L)).thenThrow(new ProviderException(503, "GEMINI_UNAVAILABLE", "Try again or choose Java."));
        mvc.perform(post(PREVIEW).param("engine", "gemini"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.issues[0].code").value("GEMINI_UNAVAILABLE"))
                .andExpect(jsonPath("$.issues[0].description").value("Try again or choose Java."));
        verifyNoInteractions(java, review);
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
