package com.example.duelingo.network

import com.example.duelingo.dto.response.UserAchievementResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface AchievementService {

    @GET("/achievements")
    suspend fun getUserAchievements(
        @Header("Authorization") token: String
    ): List<UserAchievementResponse>
}