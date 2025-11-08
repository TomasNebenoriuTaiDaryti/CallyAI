package com.example.callyaibackend.service;

import com.example.callyaibackend.model.FoodResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class FoodApiService {

    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String deepseekUrl;

    @Value("${deepseek.api.key:}")
    private String deepseekKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private static final ObjectMapper M = new ObjectMapper();

    public FoodResponse search(String query) {
        if (deepseekKey == null || deepseekKey.isBlank()) {
            int cal = switch (query.trim().toLowerCase()) {
                case "apple" -> 52;
                case "banana" -> 89;
                case "chicken breast" -> 165;
                default -> 100;
            };
            return new FoodResponse(cap(query), cal, "per 100 g", "fallback");
        }

        try {
            ObjectNode root = M.createObjectNode();
            root.put("model", model);

            ArrayNode msgs = root.putArray("messages");
            ObjectNode sys = M.createObjectNode();
            sys.put("role", "system");
            sys.put("content",
                    "You are a nutrition assistant. Reply ONLY with a compact JSON object: " +
                            "{\"name\":\"<food>\",\"calories\":<integer>,\"unit\":\"per 100 g\"} . " +
                            "Do not include any extra text.");
            msgs.add(sys);

            ObjectNode user = M.createObjectNode();
            user.put("role", "user");
            user.put("content", "Food: \"" + query + "\"");
            msgs.add(user);

            String body = M.writeValueAsString(root);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(deepseekUrl))
                    .header("Authorization", "Bearer " + deepseekKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 300) {
                throw new RuntimeException("DeepSeek error: " + resp.statusCode() + " -> " + resp.body());
            }

            JsonNode json = M.readTree(resp.body());
            String content = json.path("choices").get(0).path("message").path("content").asText("{}");

            JsonNode j = M.readTree(content);
            String name = j.path("name").asText(query);
            int calories = j.path("calories").asInt(0);
            String unit = j.path("unit").asText("per 100 g");

            return new FoodResponse(cap(name), calories, unit, "deepseek");
        } catch (Exception e) {
            throw new RuntimeException("Nepavyko gauti kalorijų iš DeepSeek", e);
        }
    }

    private static String cap(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
