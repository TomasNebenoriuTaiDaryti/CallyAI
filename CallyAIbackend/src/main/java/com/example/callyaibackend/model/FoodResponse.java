package com.example.callyaibackend.model;

public class FoodResponse {
    private String name;
    private int calories;
    private String unit;   // pvz. "per 100 g"
    private String source; // "deepseek" arba "fallback"

    public FoodResponse() { }

    public FoodResponse(String name, int calories, String unit, String source) {
        this.name = name;
        this.calories = calories;
        this.unit = unit;
        this.source = source;
    }

    public String getName() { return name; }
    public int getCalories() { return calories; }
    public String getUnit() { return unit; }
    public String getSource() { return source; }

    public void setName(String name) { this.name = name; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setSource(String source) { this.source = source; }
}
