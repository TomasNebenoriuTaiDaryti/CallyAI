package com.example.callyaibackend.model;

import java.util.List;

public class FoodResponse {
    public List<Item> items;

    public static class Item {
        public String name;
        public double calories;
    }
}
