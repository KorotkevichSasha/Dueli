package com.example.duelingo.network

import com.example.duelingo.dto.request.DuelFinishRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DuelService {

    @POST("duels/finish")
    suspend fun finishDuel(@Body request: DuelFinishRequest, @Header("Authorization") token: String): Response<Unit>
}