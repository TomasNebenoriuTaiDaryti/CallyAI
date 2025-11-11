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
                            "{\"dailyCalories\":<integer>}. " +
                            "The calorie value must be an integer for daily energy needs in kcal. No extra text.");

            msgs.add(sys);

            ObjectNode user = M.createObjectNode();
            user.put("role", "user");
            user.put("content",
                    "Goal: " + req.getGoal().toLowerCase() + ". " +
                            "Weight: " + req.getWeightKg() + " kg. " +
                            "Height: " + req.getHeightCm() + " cm. " +
                            "Gender: " + req.getGender().toLowerCase() + ". " +
                            "Activity level: " + req.getActivityLevel().toLowerCase() + ". " +
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

            if (calories <= 0) {
                return fallback(req);
            }

            return new CaloriePlanResponse(calories);
        } catch (Exception e) {
            throw new RuntimeException("Nepavyko gauti kalorijų rekomendacijos", e);
        }
    }

    private CaloriePlanResponse fallback(CaloriePlanRequest req) {
        final double assumedAge = 30.0; // approximate age when not provided by the client
        double bmr = 10.0 * req.getWeightKg() + 6.25 * req.getHeightCm() - 5.0 * assumedAge;
        if ("male".equalsIgnoreCase(req.getGender())) {
            bmr += 5.0;
        } else {
            bmr -= 161.0;
        }

        double multiplier = switch (req.getActivityLevel().toLowerCase()) {
            case "moderate" -> 1.375;
            case "active" -> 1.55;
            case "very_active" -> 1.725;
            default -> 1.2;
        };

        double maintenance = bmr * multiplier;
        String goal = req.getGoal().toLowerCase();
        double adjustment = 0.0;
        if (goal.contains("lose")) {
            adjustment = -400.0;
        } else if (goal.contains("gain")) {
            adjustment = 350.0;
        }

        double estimated = maintenance + adjustment;
        int calories = (int) Math.max(1200, Math.min(4500, Math.round(estimated)));
        return new CaloriePlanResponse(calories);
    }
}
