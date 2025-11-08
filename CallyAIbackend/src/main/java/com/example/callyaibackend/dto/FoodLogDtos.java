package com.example.callyaibackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FoodLogDtos {
    public static class ItemReq {
        public String name;
        public int calories;
        public int quantity;
    }

    public static class CreateReq {
        public String consumedAt; // "yyyy-MM-dd'T'HH:mm:ss"
        public List<ItemReq> items;
    }

    public static class EntryRes {
        public long id;
        public String name;
        public int calories;
        public int quantity;
        public int totalCalories;
        public String consumedAt;
    }
}
