package com.university.exam_lecture_timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.university.exam_lecture_timetable.exception.GeminiProviderException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AiServiceClientTests {

    private HttpServer server;
    private int responseStatus;
    private String responseBody;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private URI base() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private AiServiceClient client(URI base) {
        return new AiServiceClient(JsonMapper.builder().build(), HttpClient.newHttpClient(), base);
    }

    @Test
    void readsTheServiceHealth() {
        responseStatus = 200;
        responseBody = "{\"status\":\"UP\",\"solver\":\"not-ready\",\"geminiConfigured\":false,\"extra\":1}";

        var health = client(base()).health();

        assertThat(health).isEqualTo(new AiServiceClient.Health("UP", "not-ready", false));
    }

    @Test
    void errorStatusBecomes502() {
        responseStatus = 500;
        responseBody = "{}";
        assertThatThrownBy(() -> client(base()).health())
                .isInstanceOfSatisfying(GeminiProviderException.class, error -> {
                    assertThat(error.httpStatus()).isEqualTo(502);
                    assertThat(error.code()).isEqualTo("AI_SERVICE_REQUEST_FAILED");
                });
    }

    @Test
    void invalidJsonBecomes502() {
        responseStatus = 200;
        responseBody = "not json";
        assertThatThrownBy(() -> client(base()).health())
                .isInstanceOfSatisfying(GeminiProviderException.class,
                        error -> assertThat(error.code()).isEqualTo("AI_SERVICE_INVALID_RESPONSE"));
    }

    @Test
    void stoppedServiceBecomes503() {
        URI unused = base();
        server.stop(0);
        assertThatThrownBy(() -> client(unused).health())
                .isInstanceOfSatisfying(GeminiProviderException.class, error -> {
                    assertThat(error.httpStatus()).isEqualTo(503);
                    assertThat(error.code()).isEqualTo("AI_SERVICE_UNAVAILABLE");
                });
    }
}