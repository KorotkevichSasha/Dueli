package com.example.duelingo.network

import com.example.duelingo.dto.response.TestDetailedResponse
import com.example.duelingo.dto.response.TestSummaryResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TestService {
    @GET("/tests")
    suspend fun getTestsForTopic(
        @Header("Authorization") token: String,
        @Query("topic") topic: String
    ): List<TestSummaryResponse>

    @GET("/tests/{testId}")
    suspend fun getTestById(
        @Header("Authorization") token: String,
        @Path("testId") testId: String
    ): TestDetailedResponse

    @GET("/tests/topics")
    suspend fun getUniqueTestTopics(
        @Header("Authorization") token: String
    ): List<String>
}