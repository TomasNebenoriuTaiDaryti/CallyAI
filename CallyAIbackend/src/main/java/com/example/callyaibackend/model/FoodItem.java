package com.example.callyaibackend.model;

public class FoodItem {
    private String name;
    private double calories;

    public FoodItem(String name, double calories) {
        this.name = name;
        this.calories = calories;
    }

    public String getName() { return name; }
    public double getCalories() { return calories; }

    @Override
    public String toString() {
        return name + " = " + calories + " kcal";
    }
}
