package com.example.duelingo.network

import com.example.duelingo.dto.response.DuelInHistoryResponse
import retrofit2.http.GET

interface DuelHistoryService {

    @GET("duels/history")
    suspend fun getUserDuelHistory(): List<DuelInHistoryResponse>
}