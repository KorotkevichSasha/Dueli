package com.example.duelingo.network

import com.example.duelingo.dto.response.LeaderboardResponse
import com.example.duelingo.dto.response.PaginationResponse
import com.example.duelingo.dto.response.UserInLeaderboardResponse
import retrofit2.http.GET
import retrofit2.http.Header
interface LeaderboardService {

    @GET("/leaderboard?size=50")
    suspend fun getLeaderboard(
        @Header("Authorization") token: String
    ): LeaderboardResponse

}
