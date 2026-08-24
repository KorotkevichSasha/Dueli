package com.example.duelingo.network

import com.example.duelingo.dto.response.UserAchievementResponse
import com.example.duelingo.dto.response.AchievementClaimResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface AchievementService {

    @GET("/achievements")
    suspend fun getUserAchievements(
        @Header("Authorization") token: String
    ): List<UserAchievementResponse>

    @POST("/achievements/{achievementId}/claim")
    suspend fun claimReward(
        @Header("Authorization") token: String,
        @Path("achievementId") achievementId: String
    ): AchievementClaimResponse
}
