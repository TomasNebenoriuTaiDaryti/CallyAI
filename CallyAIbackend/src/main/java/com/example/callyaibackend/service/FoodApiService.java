package com.example.callyaibackend.service;

import com.example.callyaibackend.model.FoodItem;
import com.example.callyaibackend.util.HttpClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class FoodApiService {

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = "sk-6273a3cf307f47d9ad2b6729517f5501"; // <-- PUT YOUR KEY

    public FoodItem getCaloriesForFood(String foodName) throws Exception {

        if (foodName == null || foodName.isBlank()) {
            return new FoodItem("INVALID_INPUT", 0);
        }

        String userPrompt =
                "You are a strict calorie calculator.\n" +
                        "User gives a food name (like 'banana', 'pizza 200g').\n" +
                        "If it's valid food, respond ONLY with calories as a number (no units, no words).\n" +
                        "If it's not food, respond EXACTLY: INVALID_INPUT.\n\n" +
                        "Food: " + foodName;

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("model", "deepseek-chat");
        requestJson.addProperty("stream", false);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are a nutrition and calorie calculation assistant.");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        requestJson.add("messages", messages);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + API_KEY);
        headers.put("Content-Type", "application/json");

        String response = HttpClient.post(API_URL, headers, requestJson.toString());

        JsonObject root = JsonParser.parseString(response).getAsJsonObject();
        String reply = root
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

        if (reply.equalsIgnoreCase("INVALID_INPUT")) {
            return new FoodItem("INVALID_INPUT", 0);
        }

        double calories;
        try {
            calories = Double.parseDouble(reply.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            throw new Exception("AI returned non-numeric response: " + reply);
        }

        return new FoodItem(foodName, calories);
    }
}
