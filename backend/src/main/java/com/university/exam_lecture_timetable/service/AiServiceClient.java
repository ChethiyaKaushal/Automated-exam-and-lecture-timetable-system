package com.university.exam_lecture_timetable.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.university.exam_lecture_timetable.exception.GeminiProviderException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Calls the FastAPI AI scheduling service (ai-service/). */
@Service
public class AiServiceClient {

    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(5);

    private final JsonMapper mapper;
    private final HttpClient http;
    private final URI baseUrl;

    @Autowired
    public AiServiceClient(JsonMapper mapper,
                           @Value("${timetable.ai-service.url:http://localhost:8000}") String baseUrl) {
        this(mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/"));
    }

    AiServiceClient(JsonMapper mapper, HttpClient http, URI baseUrl) {
        this.mapper = mapper;
        this.http = http;
        this.baseUrl = baseUrl;
    }

    /** GET /health on the AI service. */
    public Health health() {
        var request = HttpRequest.newBuilder(baseUrl.resolve("health")).timeout(HEALTH_TIMEOUT).GET().build();
        return read(send(request), Health.class);
    }

    private String send(HttpRequest request) {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new GeminiProviderException(502, "AI_SERVICE_REQUEST_FAILED",
                        "The AI scheduling service returned HTTP " + response.statusCode() + ".");
            }
            return response.body();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new GeminiProviderException(503, "AI_SERVICE_UNAVAILABLE", "The AI service request was interrupted.");
        } catch (HttpTimeoutException error) {
            throw new GeminiProviderException(503, "AI_SERVICE_UNAVAILABLE",
                    "The AI scheduling service did not answer in time.", error);
        } catch (ConnectException error) {
            throw new GeminiProviderException(503, "AI_SERVICE_UNAVAILABLE",
                    "The AI scheduling service is not running at " + baseUrl + ".", error);
        } catch (IOException error) {
            throw new GeminiProviderException(503, "AI_SERVICE_UNAVAILABLE",
                    "The backend could not reach the AI scheduling service.", error);
        }
    }

    private <T> T read(String body, Class<T> type) {
        try {
            return mapper.readValue(body, type);
        } catch (JacksonException error) {
            throw new GeminiProviderException(502, "AI_SERVICE_INVALID_RESPONSE",
                    "The AI scheduling service returned invalid JSON.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Health(String status, String solver, boolean geminiConfigured) {}
}