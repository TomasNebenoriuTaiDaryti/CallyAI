package com.example.callyaibackend.service;

import com.example.callyaibackend.dto.CaloriePlanDtos.CaloriePlanRequest;
import com.example.callyaibackend.dto.CaloriePlanDtos.CaloriePlanResponse;
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
public class CaloriePlanService {

    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String deepseekUrl;

    @Value("${deepseek.api.key:}")
    private String deepseekKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private static final ObjectMapper M = new ObjectMapper();

    public CaloriePlanResponse calculate(CaloriePlanRequest req) {
        if (deepseekKey == null || deepseekKey.isBlank()) {
            return fallback(req);
        }

        try {
            ObjectNode root = M.createObjectNode();
            root.put("model", model);

            ArrayNode msgs = root.putArray("messages");
            ObjectNode sys = M.createObjectNode();
            sys.put("role", "system");
            sys.put("content",
                    "You are a nutrition expert. Respond ONLY with compact JSON: " +
                            "{\"dailyCalories\":<integer>,\"advice\":\"<short guidance>\"}. " +
                            "The calorie value must be an integer for daily energy needs in kcal. " +
                            "No additional text.");
            msgs.add(sys);

            ObjectNode user = M.createObjectNode();
            user.put("role", "user");
            user.put("content",
                    "Goal: " + req.getGoal().toLowerCase() + ". " +
                            "Weight: " + req.getWeightKg() + " kg. " +
                            "Height: " + req.getHeightCm() + " cm. " +
                            "Suggest recommended daily calories for an adult with the given goal.");
            msgs.add(user);

            String body = M.writeValueAsString(root);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(deepseekUrl))
                    .header("Authorization", "Bearer " + deepseekKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 300) {
                throw new RuntimeException("DeepSeek error: " + resp.statusCode() + " -> " + resp.body());
            }

            JsonNode json = M.readTree(resp.body());
            String content = json.path("choices").path(0).path("message").path("content").asText("{}");

            JsonNode parsed = M.readTree(content);
            int calories = parsed.path("dailyCalories").asInt(0);
            String advice = parsed.path("advice").asText("");

            if (calories <= 0) {
                return fallback(req);
            }

            return new CaloriePlanResponse(calories, advice.isBlank() ? "Laikykitės subalansuotos mitybos." : advice);
        } catch (Exception e) {
            throw new RuntimeException("Nepavyko gauti kalorijų rekomendacijos", e);
        }
    }

    private CaloriePlanResponse fallback(CaloriePlanRequest req) {
        double base = req.getWeightKg() * 30.0;
        String goal = req.getGoal().toLowerCase();
        if (goal.contains("lose")) {
            base -= 300.0;
        } else if (goal.contains("gain")) {
            base += 300.0;
        }
        int calories = (int) Math.max(1200, Math.round(base));
        String advice = switch (goal) {
            case "lose" -> "Siekiant mesti svorį rinkitės mažiau kaloringus produktus ir daugiau judėkite.";
            case "gain" -> "Norint priaugti svorio valgykite daugiau baltymų ir sveikų angliavandenių.";
            default -> "Išlaikykite subalansuotą mitybą ir stebėkite savijautą.";
        };
        return new CaloriePlanResponse(calories, advice);
    }
}
