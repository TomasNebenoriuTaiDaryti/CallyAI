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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class FoodApiService {

    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String deepseekUrl;

    @Value("${deepseek.api.key:}")
    private String deepseekKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${gpt4o.api.url:https://api.openai.com/v1/chat/completions}")
    private String gpt4oUrl;

    @Value("${gpt4o.api.key:}")
    private String gpt4oKey;

    @Value("${gpt4o.model:gpt-4o-mini}")
    private String gpt4oModel;

    private static final ObjectMapper M = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();
    private static final String VISION_SYSTEM_PROMPT = ("""
            Tu esi mitybos specialistas. Analizuok pateiktą maisto nuotrauką. Grąžink tik JSON objektą be papildomo teksto.
            Struktūra: {"items": [{"name": "produkto pavadinimas", "caloriesPer100g": skaičius, "proteinPer100g": gramai,
            "fatPer100g": gramai, "carbsPer100g": gramai}]}. Jei nieko neatpažįsti, grąžink tuščią masyvą items.
            """).strip();
    private static final String VISION_USER_PROMPT = "Atpažink maistą šioje nuotraukoje ir pateik maistines reikšmes per 100 gramų.";

    public FoodResponse search(String query) {
        if (deepseekKey == null || deepseekKey.isBlank()) {
            String normalized = query.trim().toLowerCase();
            int cal;
            double protein;
            double fat;
            double carbs;
            switch (normalized) {
                case "apple" -> {
                    cal = 52;
                    protein = 0.3;
                    fat = 0.2;
                    carbs = 13.8;
                }
                case "banana" -> {
                    cal = 89;
                    protein = 1.1;
                    fat = 0.3;
                    carbs = 22.8;
                }
                case "chicken breast" -> {
                    cal = 165;
                    protein = 31.0;
                    fat = 3.6;
                    carbs = 0.0;
                }
                default -> {
                    cal = 100;
                    protein = 5.0;
                    fat = 3.0;
                    carbs = 10.0;
                }
            }
            return new FoodResponse(cap(query), cal, "per 100 g", "fallback", protein, fat, carbs);
        }

        try {
            ObjectNode root = M.createObjectNode();
            root.put("model", model);

            ArrayNode msgs = root.putArray("messages");
            ObjectNode sys = M.createObjectNode();
            sys.put("role", "system");
            sys.put("content",
                    "You are a nutrition assistant. Reply ONLY with a compact JSON object: " +
                            "{\"name\":\"<food>\",\"calories\":<integer>,\"unit\":\"per 100 g\"," +
                            "\"protein\":<grams>,\"fat\":<grams>,\"carbs\":<grams>} . " +
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

            /*HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());*/
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 300) {
                throw new RuntimeException("DeepSeek error: " + resp.statusCode() + " -> " + resp.body());
            }

            JsonNode json = M.readTree(resp.body());

            //------------------------String content = json.path("choices").get(0).path("message").path("content").asText("{}");

            JsonNode choices = json.path("choices");
            String content = "{}";
            if (choices.isArray() && choices.size() > 0) {
                content = choices.get(0).path("message").path("content").asText("{}");
            }

            JsonNode j = M.readTree(content);
            String name = j.path("name").asText(query);
            int calories = j.path("calories").asInt(0);
            String unit = j.path("unit").asText("per 100 g");

            double protein = j.path("protein").asDouble(0.0);
            double fat = j.path("fat").asDouble(0.0);
            double carbs = j.path("carbs").asDouble(0.0);

            return new FoodResponse(cap(name), calories, unit, "deepseek", protein, fat, carbs);
        } catch (Exception e) {
            throw new RuntimeException("Nepavyko gauti kalorijų iš DeepSeek", e);
        }
    }

    public List<FoodResponse> analyzePhoto(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Pateikite nuotrauką");
        }
        if (gpt4oKey == null || gpt4oKey.isBlank()) {
            throw new IllegalStateException("GPT-4o API raktas nesukonfigūruotas");
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            ObjectNode root = M.createObjectNode();
            root.put("model", gpt4oModel);
            root.put("temperature", 0.2);
            root.put("max_tokens", 700);

            ArrayNode messages = root.putArray("messages");
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            ArrayNode sysContent = sys.putArray("content");
            ObjectNode sysText = sysContent.addObject();
            sysText.put("type", "text");
            sysText.put("text", VISION_SYSTEM_PROMPT);

            ObjectNode user = messages.addObject();
            user.put("role", "user");
            ArrayNode userContent = user.putArray("content");
            ObjectNode userText = userContent.addObject();
            userText.put("type", "text");
            userText.put("text", VISION_USER_PROMPT);
            ObjectNode image = userContent.addObject();
            image.put("type", "image_url");
            image.putObject("image_url").put("url", "data:image/jpeg;base64," + base64);

            String body = M.writeValueAsString(root);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(gpt4oUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + gpt4oKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 300) {
                throw new RuntimeException("GPT-4o klaida: " + resp.statusCode() + " -> " + resp.body());
            }

            return parseVisionResponse(resp.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Nepavyko apdoroti nuotraukos", e);
        }
    }

    private List<FoodResponse> parseVisionResponse(String payload) throws Exception {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        JsonNode root = M.readTree(payload);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return List.of();
        }
        JsonNode message = choices.get(0).path("message");
        String rawContent = extractContent(message.path("content"));
        String jsonContent = extractJson(rawContent);
        if (jsonContent == null || jsonContent.isBlank()) {
            return List.of();
        }
        JsonNode data = M.readTree(jsonContent);
        JsonNode items = data.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return List.of();
        }
        List<FoodResponse> results = new ArrayList<>();
        for (JsonNode node : items) {
            if (node == null || node.isNull()) {
                continue;
            }
            String name = node.path("name").asText("Maistas");
            double caloriesValue = node.path("caloriesPer100g").asDouble(
                    node.path("calories").asDouble(0.0)
            );
            int calories = (int) Math.round(caloriesValue);
            double protein = node.path("proteinPer100g").asDouble(0.0);
            double fat = node.path("fatPer100g").asDouble(0.0);
            double carbs = node.path("carbsPer100g").asDouble(0.0);
            String unit = node.path("unit").asText("per 100 g");

            results.add(new FoodResponse(
                    cap(name),
                    Math.max(0, calories),
                    unit,
                    "gpt-4o",
                    Math.max(0.0, protein),
                    Math.max(0.0, fat),
                    Math.max(0.0, carbs)
            ));
        }
        return results;
    }

    private static String extractContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part == null || part.isNull()) {
                    continue;
                }
                if ("text".equals(part.path("type").asText())) {
                    String text = part.path("text").asText("");
                    if (!text.isBlank()) {
                        if (sb.length() > 0) {
                            sb.append('\n');
                        }
                        sb.append(text);
                    }
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static String extractJson(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        if (trimmed.startsWith("```")) {
            String withoutFence;
            if (trimmed.startsWith("```json")) {
                withoutFence = trimmed.substring(7);
            } else {
                withoutFence = trimmed.substring(3);
            }
            int endFence = withoutFence.indexOf("```");
            String jsonPart = endFence >= 0 ? withoutFence.substring(0, endFence) : withoutFence;
            jsonPart = jsonPart.trim();
            if (jsonPart.startsWith("{") && jsonPart.endsWith("}")) {
                return jsonPart;
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return null;
    }

private static String cap(String s) {
    if (s == null || s.isBlank()) return s;
    return s.substring(0,1).toUpperCase() + s.substring(1);
}
}
