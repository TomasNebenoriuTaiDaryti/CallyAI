package com.example.callyaibackend.repo;

import com.example.callyaibackend.model.FoodItem;
import java.util.ArrayList;
import java.util.List;

public class FoodRepository {
    private final List<FoodItem> eatenFood = new ArrayList<>();

    public void addFood(FoodItem item) {
        eatenFood.add(item);
    }

    public double getTotalCalories() {
        return eatenFood.stream().mapToDouble(FoodItem::getCalories).sum();
    }

    public List<FoodItem> getAllFood() {
        return eatenFood;
    }
}
