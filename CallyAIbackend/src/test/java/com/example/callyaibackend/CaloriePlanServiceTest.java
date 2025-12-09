package com.example.callyaibackend;

import com.example.callyaibackend.dto.CaloriePlanDtos.CaloriePlanRequest;
import com.example.callyaibackend.dto.CaloriePlanDtos.CaloriePlanResponse;
import com.example.callyaibackend.service.CaloriePlanService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CaloriePlanServiceTest {

    private CaloriePlanService service;

    @BeforeEach
    void setUp() {
        service = new CaloriePlanService();
        ReflectionTestUtils.setField(service, "model", "test-model");
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(service, "deepseekKey", null);
        ReflectionTestUtils.setField(service, "deepseekUrl", null);
    }

    @Test
    void calculateFallsBackWhenNoApiKey() {
        ReflectionTestUtils.setField(service, "deepseekKey", "");

        CaloriePlanRequest req = new CaloriePlanRequest();
        req.setGoal("lose");
        req.setWeightKg(60.0);
        req.setHeightCm(175.0);

        CaloriePlanResponse resp = service.calculate(req);

        assertThat(resp.getDailyCalories()).isEqualTo(1259);
    }

    @Test
    void calculateUsesRemoteResponseWhenAvailable() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            String body = "{\"choices\":[{\"message\":{\"content\":\"{\\\"dailyCalories\\\":2750}\"}}]}";
            exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();

        int port = server.getAddress().getPort();
        ReflectionTestUtils.setField(service, "deepseekUrl", "http://localhost:" + port + "/chat/completions");
        ReflectionTestUtils.setField(service, "deepseekKey", "test-key");

        CaloriePlanRequest req = new CaloriePlanRequest();
        req.setGoal("maintain");
        req.setWeightKg(82.0);
        req.setHeightCm(182.0);

        CaloriePlanResponse resp = service.calculate(req);
        server.stop(0);

        assertThat(resp.getDailyCalories()).isEqualTo(2750);
    }

    @Test
    void fallbackUsesDefaultsWhenOptionalFieldsMissing() {
        ReflectionTestUtils.setField(service, "deepseekKey", " ");

        CaloriePlanRequest req = new CaloriePlanRequest();
        req.setGoal(null);
        req.setWeightKg(70.0);
        req.setHeightCm(170.0);
        req.setGender(null);
        req.setActivityLevel(null);

        CaloriePlanResponse resp = service.calculate(req);

        assertThat(resp.getDailyCalories()).isEqualTo(1742);
    }
}