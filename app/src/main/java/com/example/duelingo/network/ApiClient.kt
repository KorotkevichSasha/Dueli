package com.example.duelingo.network

import android.content.Context
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.AppConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object ApiClient {
    private lateinit var tokenManager: TokenManager
    private val refreshLock = Any()
    private val warmupRunning = AtomicBoolean(false)

    fun initialize(context: Context) {
        tokenManager = TokenManager(context.applicationContext)
    }

    private val publicRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(baseClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy { publicRetrofit.create(AuthService::class.java) }

    /** Starts waking a sleeping production service without delaying navigation. */
    fun warmUpServer() {
        if (!warmupRunning.compareAndSet(false, true)) return

        val request = Request.Builder()
            .url("${AppConfig.BASE_URL}actuator/health/liveness")
            .get()
            .build()
        baseClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                warmupRunning.set(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                warmupRunning.set(false)
            }
        })
    }

    private val authenticatedClient: OkHttpClient by lazy {
        check(::tokenManager.isInitialized) { "ApiClient.initialize must be called from Application" }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalToken = tokenManager.getAccessToken()
                val request = chain.request().newBuilder().apply {
                    if (originalToken != null) {
                        header("Authorization", "Bearer $originalToken")
                    }
                }.build()

                val response = chain.proceed(request)
                if (response.code != 401 || request.header("X-Auth-Retry") != null) {
                    return@addInterceptor response
                }

                val accessToken = synchronized(refreshLock) {
                    val latestToken = tokenManager.getAccessToken()
                    if (latestToken != null && latestToken != originalToken) {
                        latestToken
                    } else {
                        val refreshToken = tokenManager.getRefreshToken() ?: return@synchronized null
                        runCatching {
                            runBlocking { authService.refresh(RefreshTokenRequest(refreshToken)) }
                        }.getOrNull()?.also {
                            tokenManager.saveTokens(it.accessToken, it.refreshToken)
                        }?.accessToken
                    }
                }

                if (accessToken == null) {
                    AuthSessionManager.expireSession()
                    return@addInterceptor response
                }

                response.close()
                chain.proceed(request.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .header("X-Auth-Retry", "1")
                    .build())
            }
            .connectTimeout(AppConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(AppConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    private fun baseClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(AppConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val authenticatedRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val leaderboardService: LeaderboardService by lazy { authenticatedRetrofit.create(LeaderboardService::class.java) }
    val userService: UserService by lazy { authenticatedRetrofit.create(UserService::class.java) }
    val testService: TestService by lazy { authenticatedRetrofit.create(TestService::class.java) }
    val relationshipService: RelationshipService by lazy { authenticatedRetrofit.create(RelationshipService::class.java) }
    val questionService: QuestionService by lazy { authenticatedRetrofit.create(QuestionService::class.java) }
    val wordService: WordService by lazy { authenticatedRetrofit.create(WordService::class.java) }
    val duelService: DuelService by lazy { authenticatedRetrofit.create(DuelService::class.java) }
    val achievementService: AchievementService by lazy { authenticatedRetrofit.create(AchievementService::class.java) }
    val duelHistoryService: DuelHistoryService by lazy { authenticatedRetrofit.create(DuelHistoryService::class.java) }
}
