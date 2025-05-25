package com.example.duelingo.network

import com.example.duelingo.dto.response.DuelInHistoryResponse
import com.example.duelingo.dto.response.PaginationResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DuelHistoryService {

    @GET("duels/history")
    suspend fun getUserDuelHistory(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 5
    ): PaginationResponse<DuelInHistoryResponse>
}