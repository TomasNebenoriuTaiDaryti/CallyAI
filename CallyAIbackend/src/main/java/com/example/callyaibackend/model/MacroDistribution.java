package com.example.callyaibackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
public class MacroDistribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @Min(0) @Max(100)
    private int protein;

    @Min(0) @Max(100)
    private int fat;

    @Min(0) @Max(100)
    private int carbs;

    public MacroDistribution() {}

    public MacroDistribution(User user, int protein, int fat, int carbs) {
        this.user = user;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public int getProtein() { return protein; }
    public int getFat() { return fat; }
    public int getCarbs() { return carbs; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setProtein(int protein) { this.protein = protein; }
    public void setFat(int fat) { this.fat = fat; }
    public void setCarbs(int carbs) { this.carbs = carbs; }
}