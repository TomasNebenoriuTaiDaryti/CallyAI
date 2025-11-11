package com.example.callyaibackend.model;

import java.util.List;

public class PhotoRecognitionResponse {
    private List<FoodResponse> items = List.of();

    public PhotoRecognitionResponse() {
    }

    public PhotoRecognitionResponse(List<FoodResponse> items) {
        this.items = items;
    }

    public List<FoodResponse> getItems() {
        return items;
    }

    public void setItems(List<FoodResponse> items) {
        this.items = items == null ? List.of() : items;
    }
}