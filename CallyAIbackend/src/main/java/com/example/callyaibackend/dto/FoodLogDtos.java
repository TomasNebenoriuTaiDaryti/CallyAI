package com.example.callyaibackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FoodLogDtos {
    public static class ItemReq {
        public String name;
        public int caloriesPer100g;
        public int grams;
        public int quantity;
        public Double proteinPer100g;
        public Double fatPer100g;
        public Double carbsPer100g;
    }

    public static class CreateReq {
        public String consumedAt; // "yyyy-MM-dd'T'HH:mm:ss"
        public List<ItemReq> items;
    }

    public static class EntryRes {
        public long id;
        public String name;
        public int calories;
        public int caloriesPer100g;
        public int quantity;
        public int grams;
        public int totalCalories;
        public String consumedAt;
        public double protein;
        public double fat;
        public double carbs;
        public double totalProtein;
        public double totalFat;
        public double totalCarbs;
    }
}
