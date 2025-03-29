package com.example.duelingo.network

import com.example.duelingo.dto.response.AudioAnswerResponse
import com.example.duelingo.dto.response.AudioQuestionResponse
import com.example.duelingo.dto.response.QuestionDetailedResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface QuestionService {

    @GET("/questions/random")
    suspend fun getRandomQuestions(
        @Header("Authorization") token: String,
        @Query("topic") topic: String?,
        @Query("questionDifficulty") difficulty: String?,
        @Query("size") size: Int = 10
    ): List<QuestionDetailedResponse>

    @GET("/questions/audio/random")
    suspend fun getRandomAudioQuestions(
        @Header("Authorization") token: String,
        @Query("size") size: Int = 1
    ): List<AudioQuestionResponse>

    @Multipart
    @POST("questions/verify-audio-answer")
    suspend fun verifyAnswer(
        @Header("Authorization") token: String,
        @Query("questionId") questionId: String,
        @Part audioFile: MultipartBody.Part
    ): AudioAnswerResponse
}