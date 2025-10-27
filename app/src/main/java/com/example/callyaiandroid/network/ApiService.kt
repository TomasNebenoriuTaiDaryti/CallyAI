package com.example.callyaiandroid.network

import com.example.callyaiandroid.network.dto.*
import retrofit2.http.*
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Header
import com.example.callyaiandroid.network.dto.FoodSearchRes

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterReq): AuthResp
    @POST("auth/login")
    suspend fun login(@Body body: LoginReq): AuthResp
    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") bearer: String): Map<String, String>
    @GET("auth/me")
    suspend fun me(@Header("Authorization") bearer: String): UserMe
    @PUT("auth/me")
    suspend fun updateMe(@Header("Authorization") bearer: String, @Body body: UpdateProfileReq): UserMe

    @GET("food/search")
    suspend fun searchFood(
        @Header("Authorization") auth: String,
        @Query("q") query: String
    ): FoodSearchRes
}
