package com.example.duelingo.network

import com.example.duelingo.dto.response.DuelInHistoryResponse
import com.example.duelingo.dto.response.PaginationResponse
import com.example.duelingo.dto.response.MatchmakingEstimateResponse
import com.example.duelingo.dto.response.DuelStatsResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import com.example.duelingo.dto.event.DuelChallengeEvent
import com.example.duelingo.dto.request.DuelChallengeRequest
import retrofit2.Response
import com.example.duelingo.dto.event.DuelFoundEvent

interface DuelHistoryService {

    @GET("duels/matchmaking/estimate")
    suspend fun getMatchmakingEstimate(
        @Query("difficulty") difficulty: String
    ): MatchmakingEstimateResponse

    @GET("duels/history")
    suspend fun getUserDuelHistory(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 5
    ): PaginationResponse<DuelInHistoryResponse>

    @GET("duels/stats")
    suspend fun getUserDuelStats(): DuelStatsResponse

    @POST("duels/challenges")
    suspend fun challengeFriend(@Body request: DuelChallengeRequest): DuelChallengeEvent

    @GET("duels/challenges/pending")
    suspend fun getPendingChallenges(): List<DuelChallengeEvent>

    @POST("duels/challenges/{challengeId}/respond")
    suspend fun respondToChallenge(
        @Path("challengeId") challengeId: String,
        @Query("accept") accept: Boolean
    ): Response<DuelFoundEvent>
}
