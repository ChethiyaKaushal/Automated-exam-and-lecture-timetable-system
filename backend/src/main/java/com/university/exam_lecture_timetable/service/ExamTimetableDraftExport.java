package com.university.exam_lecture_timetable.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "exam-draft.period-id")
public class ExamTimetableDraftExport implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExamTimetableDraftExport.class);

    private final ExamTimetableDraftService draftService;
    private final GeminiExamTimetableService geminiService;
    private final JsonMapper jsonMapper;
    private final String engine;
    private final Long periodId;
    private final Path output;

    public ExamTimetableDraftExport(
            ExamTimetableDraftService draftService, GeminiExamTimetableService geminiService, JsonMapper jsonMapper,
            @Value("${exam-draft.engine:java}") String engine,
            @Value("${exam-draft.period-id}") Long periodId,
            @Value("${exam-draft.output}") String output
    ) {
        this.draftService = draftService;
        this.geminiService = geminiService;
        this.engine = engine;
        this.jsonMapper = jsonMapper;
        this.periodId = periodId;
        this.output = Path.of(output).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (Files.exists(output)) {
            throw new IllegalStateException("Draft output already exists: " + output);
        }
        var draft = switch (engine) {
            case "java" -> draftService.generate(periodId);
            case "gemini" -> geminiService.generate(periodId);
            default -> throw new IllegalArgumentException("exam-draft.engine must be java or gemini.");
        };
        String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(draft);
        Files.createDirectories(output.getParent());
        Files.writeString(output, json + System.lineSeparator(),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        log.info("Draft timetable saved to {}. Scheduled: {}. Unscheduled: {}. Student memberships complete: {}.",
                output, draft.exams().size(), draft.unscheduledExams().size(), draft.studentMembershipsComplete());
    }
}
