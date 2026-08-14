package com.example.duelingo.utils

import com.example.duelingo.dto.response.LeaderboardResponse
import com.example.duelingo.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Keeps the latest ranking ready so opening the tab never starts with an empty list. */
object LeaderboardCache {
    private const val FRESH_FOR_MS = 45_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var response: LeaderboardResponse? = null
    @Volatile private var tokenKey: Int? = null
    @Volatile private var updatedAt = 0L
    @Volatile private var loading = false

    fun current(accessToken: String?): LeaderboardResponse? {
        if (accessToken == null || tokenKey != accessToken.hashCode()) return null
        return response
    }

    fun store(accessToken: String, value: LeaderboardResponse) {
        tokenKey = accessToken.hashCode()
        response = value
        updatedAt = System.currentTimeMillis()
    }

    fun prefetch(accessToken: String?) {
        accessToken ?: return
        val fresh = current(accessToken) != null &&
            System.currentTimeMillis() - updatedAt < FRESH_FOR_MS
        if (fresh || loading) return

        loading = true
        scope.launch {
            runCatching {
                ApiClient.leaderboardService.getLeaderboard("Bearer $accessToken")
            }.onSuccess { store(accessToken, it) }
            loading = false
        }
    }
}
