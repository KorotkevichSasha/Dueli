package com.example.duelingo.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "http://192.168.100.8:8082"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthService = retrofit.create(AuthService::class.java)
    val leaderboardService: LeaderboardService = retrofit.create(LeaderboardService::class.java)
    val userService: UserService = retrofit.create(UserService::class.java)
    val testService: TestService = retrofit.create(TestService::class.java)
    val questionService: QuestionService = retrofit.create(QuestionService::class.java)
    val relationshipService: RelationshipService = retrofit.create(RelationshipService::class.java)
}