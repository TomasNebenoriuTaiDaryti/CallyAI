package com.example.callyaibackend;

import com.example.callyaibackend.model.FoodResponse;
import com.example.callyaibackend.service.FoodApiService;
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

class FoodApiServiceTest {

    private FoodApiService service;

    @BeforeEach
    void setUp() {
        service = new FoodApiService();
        ReflectionTestUtils.setField(service, "model", "test-model");
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(service, "deepseekKey", null);
        ReflectionTestUtils.setField(service, "deepseekUrl", null);
    }

    @Test
    void searchReturnsFallbackWhenNoApiKey() {
        ReflectionTestUtils.setField(service, "deepseekKey", "");

        FoodResponse resp = service.search("Banana");

        assertThat(resp.getSource()).isEqualTo("fallback");
        assertThat(resp.getCalories()).isEqualTo(89);
        assertThat(resp.getName()).isEqualTo("Banana");
    }

    @Test
    void searchParsesRemoteResponse() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            String body = "{\"choices\":[{\"message\":{\"content\":\"{\\\"name\\\":\\\"Mango\\\",\\\"calories\\\":135,\\\"unit\\\":\\\"per 100 g\\\",\\\"protein\\\":1.2,\\\"fat\\\":0.6,\\\"carbs\\\":35.0}\"}}]}";
            exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();

        int port = server.getAddress().getPort();
        ReflectionTestUtils.setField(service, "deepseekUrl", "http://localhost:" + port + "/chat/completions");
        ReflectionTestUtils.setField(service, "deepseekKey", "key");

        FoodResponse resp = service.search("mango");
        server.stop(0);

        assertThat(resp.getSource()).isEqualTo("deepseek");
        assertThat(resp.getName()).isEqualTo("Mango");
        assertThat(resp.getCalories()).isEqualTo(135);
        assertThat(resp.getProtein()).isEqualTo(1.2);
        assertThat(resp.getCarbs()).isEqualTo(35.0);
    }
}