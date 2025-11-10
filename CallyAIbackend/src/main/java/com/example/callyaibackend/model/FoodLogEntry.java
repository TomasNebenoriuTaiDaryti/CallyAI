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
    private double proteinPer100g;
    private double fatPer100g;
    private double carbsPer100g;
    private double protein;
    private double fat;
    private double carbs;
    private double totalProtein;
    private double totalFat;
    private double totalCarbs;

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
        public double getProteinPer100g() { return proteinPer100g; }
        public double getFatPer100g() { return fatPer100g; }
        public double getCarbsPer100g() { return carbsPer100g; }
        public double getProtein() { return protein; }
        public double getFat() { return fat; }
        public double getCarbs() { return carbs; }
        public double getTotalProtein() { return totalProtein; }
        public double getTotalFat() { return totalFat; }
        public double getTotalCarbs() { return totalCarbs; }

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
            recalcTotalMacros();
        }

        public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }

        public void setProteinPer100g(double proteinPer100g) {
            this.proteinPer100g = proteinPer100g;
            recalcMacros();
        }

        public void setFatPer100g(double fatPer100g) {
            this.fatPer100g = fatPer100g;
            recalcMacros();
        }

        public void setCarbsPer100g(double carbsPer100g) {
            this.carbsPer100g = carbsPer100g;
            recalcMacros();
        }

        private int calculatePortionCalories() {
            if (grams <= 0 || caloriesPer100g <= 0) {
                return 0;
            }
            return Math.round((caloriesPer100g * grams) / 100.0f);
        }

        private void recalcPortionAndTotal() {
            this.calories = calculatePortionCalories();
            recalcTotalCalories();
            recalcMacros();
        }

        private void recalcTotalCalories() {
            this.totalCalories = this.calories * this.quantity;
        }

        private double calculateMacro(double per100g) {
            if (grams <= 0 || per100g <= 0) {
                return 0.0;
            }
            return Math.round((per100g * grams) / 100.0 * 10.0) / 10.0;
        }

        private void recalcMacros() {
            this.protein = calculateMacro(proteinPer100g);
            this.fat = calculateMacro(fatPer100g);
            this.carbs = calculateMacro(carbsPer100g);
            recalcTotalMacros();
        }

        private void recalcTotalMacros() {
            this.totalProtein = Math.round(this.protein * this.quantity * 10.0) / 10.0;
            this.totalFat = Math.round(this.fat * this.quantity * 10.0) / 10.0;
            this.totalCarbs = Math.round(this.carbs * this.quantity * 10.0) / 10.0;
        }
    }