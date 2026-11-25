package com.university.exam_lecture_timetable.controller;

import com.university.exam_lecture_timetable.service.AiServiceClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin check that Spring Boot can reach the AI scheduling service. */
@RestController
@RequestMapping("/api/admin/ai-service")
public class AiServiceController {

    private final AiServiceClient client;

    public AiServiceController(AiServiceClient client) {
        this.client = client;
    }

    @GetMapping("/health")
    public AiServiceClient.Health health() {
        return client.health();
    }
}