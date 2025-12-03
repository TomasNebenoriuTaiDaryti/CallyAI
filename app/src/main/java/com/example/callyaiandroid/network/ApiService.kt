package com.example.callyaiandroid.network

import com.example.callyaiandroid.network.dto.*
import retrofit2.http.*
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Header
import com.example.callyaiandroid.network.dto.FoodSearchRes
import okhttp3.MultipartBody
import com.example.callyaiandroid.data.MacroPercents

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

    @GET("macros")
    suspend fun getMacroPercents(@Header("Authorization") bearer: String): MacroPercents

    @POST("macros")
    suspend fun saveMacroPercents(
        @Header("Authorization") bearer: String,
        @Body body: MacroPercents
    ): MacroPercents
    @POST("auth/calories/plan")
    suspend fun calculateDailyCalories(
        @Header("Authorization") auth: String,
        @Body body: CaloriePlanReq
    ): CaloriePlanResp

    @GET("food/search")
    suspend fun searchFood(
        @Header("Authorization") auth: String,
        @Query("q") query: String
    ): FoodSearchRes
    @POST("diary/log")
    suspend fun createFoodLog(
        @Header("Authorization") auth: String,
        @Body body: FoodLogCreateReq
    ): Unit

    @PUT("diary/log/{id}")
    suspend fun updateFoodLog(
        @Header("Authorization") auth: String,
        @Path("id") id: Long,
        @Body body: FoodLogUpdateReq
    ): FoodLogEntryRes

    @GET("diary/day")
    suspend fun getDayEntries(
        @Header("Authorization") auth: String,
        @Query("date") date: String
    ): List<FoodLogEntryRes>

    @GET("diary/all")
    suspend fun diaryAll(
        @Header("Authorization") auth: String
    ): List<FoodLogDtos>

    @POST("auth/forgot")
    suspend fun forgotPassword(@Body body: ForgotPasswordReq): Map<String, String>

    @DELETE("diary/log/{id}")
    suspend fun deleteFoodLog(
        @Header("Authorization") auth: String,
        @Path("id") id: Long
    ): Unit

    @Multipart
    @POST("food/photo")
    suspend fun analyzeFoodPhoto(
        @Header("Authorization") auth: String,
        @Part image: MultipartBody.Part
    ): PhotoRecognitionRes

}
