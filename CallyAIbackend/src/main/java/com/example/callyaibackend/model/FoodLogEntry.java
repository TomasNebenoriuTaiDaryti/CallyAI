package com.example.callyaibackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class FoodLogEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    private String name;
    private int calories;       // per unit
    private int quantity;
    private int totalCalories;  // calories * quantity
    private LocalDateTime consumedAt;

    public FoodLogEntry() {}

    public FoodLogEntry(User user, String name, int calories, int quantity, LocalDateTime consumedAt) {
        this.user = user;
        this.name = name;
        this.calories = calories;
        this.quantity = quantity;
        this.totalCalories = calories * quantity;
        this.consumedAt = consumedAt;
    }

    // getters/setters ...
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public int getCalories() { return calories; }
    public int getQuantity() { return quantity; }
    public int getTotalCalories() { return totalCalories; }
    public LocalDateTime getConsumedAt() { return consumedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setName(String name) { this.name = name; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setQuantity(int quantity) { this.quantity = quantity; this.totalCalories = calories * quantity; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
}
