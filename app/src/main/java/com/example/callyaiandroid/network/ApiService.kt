package com.example.callyaiandroid.network

import com.example.callyaiandroid.model.User
import retrofit2.http.*

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>

    @POST("users")
    suspend fun createUser(@Body user: User): User
}