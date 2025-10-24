package com.example.callyaiandroid.network.dto

data class RegisterReq(
    val name: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val dailyCalories: Int = 2000
)

data class LoginReq(val email: String, val password: String)

data class UserMe(
    val id: Long,
    val name: String,
    val email: String,
    val dailyCalories: Int
)

data class UpdateProfileReq(
    val name: String,
    val email: String,
    val dailyCalories: Int
)

data class AuthResp(
    val token: String,
    val userId: Long,
    val name: String
)
