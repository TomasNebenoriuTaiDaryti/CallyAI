package com.example.callyaiandroid.network.dto

import com.google.gson.annotations.SerializedName
data class RegisterReq(
    val name: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)

data class LoginReq(val email: String, val password: String)

data class UserMe(
    val id: Long,
    val name: String,
    val email: String,
    val dailyCalories: Int?,
    val units: String? = null,
    val theme: String? = null,
    val autoAddAi: Boolean? = null
)

data class UpdateProfileReq(
    val name: String,
    val email: String,
    val dailyCalories: Int
)

data class CaloriePlanReq(
    val goal: String,
    val weightKg: Double,
    val heightCm: Double
)

data class CaloriePlanResp(
    val dailyCalories: Int,
    val advice: String
)

data class AuthResp(
    val token: String,
    val userId: Long,
    val name: String
)

data class FoodSearchRes(
    val calories: Int,
    val name: String? = null,
    val unit: String? = null,
    val source: String? = null
)

data class FoodCartItem(
    val name: String,
    val calories: Int,
    var quantity: Int = 1
)

data class FoodLogItemReq(
    val name: String,
    val caloriesPer100g: Int,
    val grams: Int,
    val quantity: Int
)

data class FoodLogCreateReq(
    val consumedAt: String,
    val items: List<FoodLogItemReq>
)

data class FoodLogEntryRes(
    val id: Long,
    val name: String,
    @SerializedName("calories")
    val portionCalories: Int,
    val caloriesPer100g: Int,
    val grams: Int,
    val quantity: Int,
    val totalCalories: Int,
    val consumedAt: String
)

data class FoodLogUpdateReq(
    val grams: Int,
    val quantity: Int
)