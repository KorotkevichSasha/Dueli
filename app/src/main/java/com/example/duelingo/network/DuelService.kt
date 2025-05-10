package com.example.duelingo.network

import com.example.duelingo.dto.request.DuelFinishRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DuelService {
    @POST("/duel/finish")
    suspend fun finishDuel(
        @Header("Authorization") token: String,
        @Body request: DuelFinishRequest
    ): Response<Unit>
}