package com.university.exam_lecture_timetable.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.university.exam_lecture_timetable.exception.GeminiProviderException;
import com.university.exam_lecture_timetable.exception.GlobalExceptionHandler;
import com.university.exam_lecture_timetable.service.AiServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AiServiceControllerTests {

    private final AiServiceClient client = mock(AiServiceClient.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new AiServiceController(client))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void showsTheAiServiceHealth() throws Exception {
        when(client.health()).thenReturn(new AiServiceClient.Health("UP", "not-ready", false));

        mvc.perform(get("/api/admin/ai-service/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.solver").value("not-ready"))
                .andExpect(jsonPath("$.geminiConfigured").value(false));
    }

    @Test
    void unreachableServiceShowsAReadable503() throws Exception {
        when(client.health()).thenThrow(new GeminiProviderException(503, "AI_SERVICE_UNAVAILABLE", "Not running."));

        mvc.perform(get("/api/admin/ai-service/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.issues[0].code").value("AI_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.issues[0].description").value("Not running."));
    }
}