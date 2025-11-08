package com.example.callyaiandroid.network.dto

import com.google.gson.annotations.SerializedName

data class FoodLogDtos(
    val id: Long,
    val name: String,
    val calories: Int,
    val quantity: Int,
    @SerializedName(value = "totalCalories", alternate = ["total_calories"])
    val totalCalories: Int?,
    @SerializedName(value = "consumedAt", alternate = ["consumed_at"])
    val consumedAt: String
)
