package com.example.duelingo.network

import com.example.duelingo.dto.request.DuelFinishRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface DuelService {

    @POST("duels/finish")
    suspend fun finishDuel(@Body request: DuelFinishRequest, @Header("Authorization") token: String): Response<Unit>

    @POST("duels/{duelId}/forfeit")
    suspend fun forfeitDuel(
        @Path("duelId") duelId: String,
        @Header("Authorization") token: String
    ): Response<Unit>
}
