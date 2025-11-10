package com.example.callyaibackend.model;

public class FoodResponse {
    private String name;
    private int calories;
    private String unit;
    private String source;
    private double protein;
    private double fat;
    private double carbs;

    public FoodResponse() { }

    public FoodResponse(String name, int calories, String unit, String source, double protein, double fat, double carbs) {
        this.name = name;
        this.calories = calories;
        this.unit = unit;
        this.source = source;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
    }

    public String getName() { return name; }
    public int getCalories() { return calories; }
    public String getUnit() { return unit; }
    public String getSource() { return source; }
    public double getProtein() { return protein; }
    public double getFat() { return fat; }
    public double getCarbs() { return carbs; }

    public void setName(String name) { this.name = name; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setSource(String source) { this.source = source; }
    public void setProtein(double protein) { this.protein = protein; }
    public void setFat(double fat) { this.fat = fat; }
    public void setCarbs(double carbs) { this.carbs = carbs; }
}
