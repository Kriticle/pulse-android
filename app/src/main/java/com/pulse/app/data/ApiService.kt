package com.pulse.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("levels")
    suspend fun getLevels(): List<LevelDto>

    @GET("levels/{id}")
    suspend fun getLevel(@Path("id") id: String): LevelDto

    @POST("attempts")
    suspend fun postAttempt(@Body request: CreateAttemptRequest): AttemptResponse

    @GET("best-attempts")
    suspend fun getBestAttempts(@Query("userId") userId: String): List<BestAttemptDto>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto

    @POST("users")
    suspend fun upsertUser(@Body request: UpsertUserRequest): UserDto
}
