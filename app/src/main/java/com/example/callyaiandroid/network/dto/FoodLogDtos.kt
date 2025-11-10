package com.example.callyaiandroid.network.dto

import com.google.gson.annotations.SerializedName

data class FoodLogDtos(
    val id: Long,
    val name: String,
    @SerializedName("calories")
    val portionCalories: Int,
    @SerializedName("caloriesPer100g")
    val caloriesPer100g: Int,
    val grams: Int,
    val quantity: Int,
    @SerializedName(value = "totalCalories", alternate = ["total_calories"])
    val totalCalories: Int?,
    @SerializedName(value = "consumedAt", alternate = ["consumed_at"])
    val consumedAt: String,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    @SerializedName(value = "totalProtein", alternate = ["total_protein"])
    val totalProtein: Double,
    @SerializedName(value = "totalFat", alternate = ["total_fat"])
    val totalFat: Double,
    @SerializedName(value = "totalCarbs", alternate = ["total_carbs"])
    val totalCarbs: Double,
)
