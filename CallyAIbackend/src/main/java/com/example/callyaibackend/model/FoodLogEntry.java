package com.example.callyaibackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class FoodLogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    private String name;
    private int calories;
    private int caloriesPer100g;
    private int grams;
    private int quantity;
    private int totalCalories;
    private LocalDateTime consumedAt;

    public FoodLogEntry() {}

    public FoodLogEntry(User user, String name, int caloriesPer100g, int grams, int quantity, LocalDateTime consumedAt) {
            this.user = user;
            this.name = name;
            this.caloriesPer100g = caloriesPer100g;
            this.grams = grams;
            this.quantity = quantity;
            this.consumedAt = consumedAt;
            recalcPortionAndTotal();
        }

        public Long getId() { return id; }
        public User getUser() { return user; }
        public String getName() { return name; }
        public int getCalories() { return calories; }
        public int getCaloriesPer100g() { return caloriesPer100g; }
        public int getGrams() { return grams; }
        public int getQuantity() { return quantity; }
        public int getTotalCalories() { return totalCalories; }
        public LocalDateTime getConsumedAt() { return consumedAt; }

        public void setId(Long id) { this.id = id; }
        public void setUser(User user) { this.user = user; }
        public void setName(String name) { this.name = name; }

        public void setCalories(int calories) {
            this.calories = calories;
            recalcTotalCalories();
        }

        public void setCaloriesPer100g(int caloriesPer100g) {
            this.caloriesPer100g = caloriesPer100g;
            recalcPortionAndTotal();
        }

        public void setGrams(int grams) {
            this.grams = grams;
            recalcPortionAndTotal();
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
            recalcTotalCalories();
        }

        public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }

        private int calculatePortionCalories() {
            if (grams <= 0 || caloriesPer100g <= 0) {
                return 0;
            }
            return Math.round((caloriesPer100g * grams) / 100.0f);
        }

        private void recalcPortionAndTotal() {
            this.calories = calculatePortionCalories();
            recalcTotalCalories();
        }

        private void recalcTotalCalories() {
            this.totalCalories = this.calories * this.quantity;
        }
    }