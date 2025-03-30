package com.example.duelingo.network

import com.example.duelingo.dto.request.AddWordRequest
import com.example.duelingo.dto.response.WordProgressResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

interface WordService {
    @GET("words/due")
    suspend fun getDueWords(): List<WordProgressResponse>

    @POST("words")
    suspend fun addWord(@Body request: AddWordRequest)

    @POST("words/{wordId}/review")
    suspend fun reviewWord(
        @Path("wordId") wordId: UUID,
        @Query("quality")  quality: Int
    )
}