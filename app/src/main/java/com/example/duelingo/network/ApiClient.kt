package com.example.duelingo.network

import com.example.duelingo.utils.AppConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy { retrofit.create(AuthService::class.java) }
    val leaderboardService: LeaderboardService by lazy { retrofit.create(LeaderboardService::class.java) }
    val userService: UserService by lazy { retrofit.create(UserService::class.java) }
    val testService: TestService by lazy { retrofit.create(TestService::class.java) }
    val relationshipService: RelationshipService by lazy { retrofit.create(RelationshipService::class.java) }
    val questionService: QuestionService by lazy { retrofit.create(QuestionService::class.java) }
    val wordService: WordService by lazy { retrofit.create(WordService::class.java) }
}