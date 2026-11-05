package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.exception.GeminiProviderException;
import com.university.exam_lecture_timetable.validation.DrawingExamRule;
import com.university.exam_lecture_timetable.validation.GeminiExamTimetableValidator;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.Draft;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GeminiExamTimetableService {

    private static final Logger log = LoggerFactory.getLogger(GeminiExamTimetableService.class);
    private static final List<Duration> UNAVAILABLE_RETRY_DELAYS =
            List.of(Duration.ofSeconds(5), Duration.ofSeconds(10));

    private static final String RESPONSE_SCHEMA = """
            {
              "type": "object",
              "required": ["status", "exams", "reason"],
              "properties": {
                "status": {"type": "string", "enum": ["SCHEDULED", "UNSCHEDULED"]},
                "reason": {"type": ["string", "null"]},
                "exams": {"type": "array", "items": {
                  "type": "object", "required": ["requirementId", "timeslotIds"],
                  "properties": {
                    "requirementId": {"type": "integer"},
                    "timeslotIds": {"type": "array", "items": {"type": "integer"}}
                  }
                }}
              }
            }
            """;

    private final ExamSchedulingInputService inputs;
    private final ExamTimetablePromptBuilder prompts;
    private final ExamTimetableDraftService javaDrafts;
    private final JsonMapper mapper;
    private final String apiKey;
    private final String model;

    public GeminiExamTimetableService(
            ExamSchedulingInputService inputs, ExamTimetablePromptBuilder prompts,
            ExamTimetableDraftService javaDrafts, JsonMapper mapper,
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:gemini-3.6-flash}") String model
    ) {
        this.inputs = inputs;
        this.prompts = prompts;
        this.javaDrafts = javaDrafts;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public Draft generate(Long periodId) {
        if (apiKey.isBlank()) {
            throw new GeminiProviderException(503, "GEMINI_NOT_CONFIGURED",
                    "Set GEMINI_API_KEY on the backend before generating with Gemini.");
        }
        if (!model.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException("GEMINI_MODEL must be a Gemini model ID without the models/ prefix.");
        }

        var loaded = inputs.loadDraft(periodId);
        var drawing = DrawingExamRule.assignments(loaded.input());
        Draft baseline = javaDrafts.generate(loaded);
        String prompt = prompts.build(loaded.input(), drawing, loaded.membershipComplete(),
                baseline.allExamsPlaced() ? baseline.exams() : List.of());
        JsonNode proposal;
        try {
            proposal = mapper.readTree(requestProposal(prompt));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new GeminiProviderException(503, "GEMINI_UNAVAILABLE", "The Gemini request was interrupted.");
        } catch (IOException error) {
            throw new GeminiProviderException(503, "GEMINI_UNAVAILABLE",
                    "The backend could not reach Gemini. Try again or choose Java.");
        } catch (JacksonException error) {
            throw new GeminiProviderException(502, "GEMINI_INVALID_RESPONSE",
                    "Gemini returned invalid JSON. Try again or choose Java.");
        }
        return GeminiExamTimetableValidator.validate(loaded, drawing, proposal);
    }

    private String requestProposal(String prompt) throws IOException, InterruptedException {
        var payload = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", mapper.readTree(RESPONSE_SCHEMA)));
        var request = HttpRequest.newBuilder(
                        URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();
        var response = sendWithUnavailableRetries(request);
        if (response.statusCode() == 503) {
            throw new GeminiProviderException(503, "GEMINI_UNAVAILABLE",
                    "Gemini is still unavailable after three attempts. Try again later or choose Java.");
        }
        if (response.statusCode() != 200) {
            throw new GeminiProviderException(502, "GEMINI_REQUEST_FAILED",
                    "Gemini rejected the request (HTTP " + response.statusCode() + ").");
        }
        JsonNode candidate = mapper.readTree(response.body()).path("candidates").path(0);
        String finishReason = candidate.path("finishReason").asText("");
        if (!"STOP".equals(finishReason)) {
            throw new GeminiProviderException(502, "GEMINI_INCOMPLETE_RESPONSE",
                    "Gemini did not return a complete proposal. Finish reason: "
                    + (finishReason.isBlank() ? "no candidate" : finishReason) + ".");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (!part.path("thought").asBoolean(false)) {
                text.append(part.path("text").asText(""));
            }
        }
        if (text.isEmpty()) {
            throw new GeminiProviderException(502, "GEMINI_INCOMPLETE_RESPONSE", "Gemini returned an empty proposal.");
        }
        return text.toString();
    }

    private HttpResponse<String> sendWithUnavailableRetries(HttpRequest request)
            throws IOException, InterruptedException {
        HttpClient http = GeminiHttpClient.CLIENT;
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        for (Duration delay : UNAVAILABLE_RETRY_DELAYS) {
            if (response.statusCode() != 503) {
                break;
            }
            log.warn("Gemini returned HTTP 503. Retrying in {} seconds.", delay.toSeconds());
            Thread.sleep(delay);
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        }
        return response;
    }

    private static class GeminiHttpClient {
        private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }
}
