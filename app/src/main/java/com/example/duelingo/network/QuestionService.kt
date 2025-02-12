package com.example.duelingo.network

import com.example.duelingo.dto.response.QuestionDetailedResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface QuestionService {

    @GET("/questions/random")
    suspend fun getRandomQuestions(
        @Header("Authorization") token: String,
        @Query("topic") topic: String?,
        @Query("questionDifficulty") difficulty: String?,
        @Query("size") size: Int = 10
    ): List<QuestionDetailedResponse>
}