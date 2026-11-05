package com.university.exam_lecture_timetable.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
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
    private static final Path RUNS_DIRECTORY = Path.of("docs", "timetables", "runs");

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
            @Value("${exam-draft.output:}") String output
    ) {
        this.draftService = draftService;
        this.geminiService = geminiService;
        this.engine = engine;
        this.jsonMapper = jsonMapper;
        this.periodId = periodId;
        this.output = output.isBlank() ? null : Path.of(output).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (output != null && Files.exists(output)) {
            throw new IllegalStateException("Draft output already exists: " + output);
        }
        var draft = switch (engine) {
            case "java" -> draftService.generate(periodId);
            case "gemini" -> geminiService.generate(periodId);
            default -> throw new IllegalArgumentException("exam-draft.engine must be java or gemini.");
        };
        String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(draft) + System.lineSeparator();
        Path saved = writeDraft(json);
        log.info("Draft timetable saved to {}. Scheduled: {}. Unscheduled: {}. Student memberships complete: {}.",
                saved, draft.exams().size(), draft.unscheduledExams().size(), draft.studentMembershipsComplete());
    }

    private Path writeDraft(String json) throws IOException {
        if (output != null) {
            Files.createDirectories(output.getParent());
            return Files.writeString(output, json, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }

        Path directory = Files.createDirectories(RUNS_DIRECTORY.toAbsolutePath().normalize());
        String timestamp = Instant.now().toString().replace(':', '-');
        String prefix = "exam-timetable-period-" + periodId + "-" + engine + "-" + timestamp + "-";
        Path temporary = Files.createTempFile(directory, prefix, ".tmp");
        Path saved = temporary.resolveSibling(temporary.getFileName().toString().replace(".tmp", ".json"));
        try {
            Files.writeString(temporary, json);
            return Files.move(temporary, saved);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
