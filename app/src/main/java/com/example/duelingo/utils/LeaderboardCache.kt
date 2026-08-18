package com.example.duelingo.utils

import android.content.Context
import com.example.duelingo.dto.response.LeaderboardResponse
import com.example.duelingo.network.ApiClient
import com.google.gson.Gson
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

    fun clearMemory() {
        response = null
        tokenKey = null
        updatedAt = 0L
        loading = false
    }

    fun current(context: Context, accessToken: String?): LeaderboardResponse? {
        if (accessToken == null) return null
        val stableKey = CacheIdentity.key(accessToken)
        if (tokenKey == stableKey.hashCode()) response?.let { return it }
        val json = context.getSharedPreferences("duelrush_leaderboard_cache", Context.MODE_PRIVATE)
            .getString(stableKey, null) ?: return null
        return runCatching { Gson().fromJson(json, LeaderboardResponse::class.java) }
            .getOrNull()
            ?.also {
                response = it
                tokenKey = stableKey.hashCode()
            }
    }

    fun store(context: Context, accessToken: String, value: LeaderboardResponse) {
        val stableKey = CacheIdentity.key(accessToken)
        tokenKey = stableKey.hashCode()
        response = value
        updatedAt = System.currentTimeMillis()
        context.getSharedPreferences("duelrush_leaderboard_cache", Context.MODE_PRIVATE)
            .edit()
            .putString(stableKey, Gson().toJson(value))
            .apply()
    }

    fun prefetch(context: Context, accessToken: String?) {
        accessToken ?: return
        val fresh = current(context, accessToken) != null &&
            System.currentTimeMillis() - updatedAt < FRESH_FOR_MS
        if (fresh || loading) return

        loading = true
        scope.launch {
            runCatching {
                ApiClient.leaderboardService.getLeaderboard("Bearer $accessToken")
            }.onSuccess { store(context.applicationContext, accessToken, it) }
            loading = false
        }
    }
}
