package com.example.duelingo.utils

import android.content.Context
import com.example.duelingo.dto.response.DuelInHistoryResponse
import com.example.duelingo.dto.response.DuelStatsResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DuelCache {
    private const val PREFS = "duelrush_duel_cache"
    private val gson = Gson()

    fun readHistory(context: Context, token: String?): List<DuelInHistoryResponse> {
        token ?: return emptyList()
        val json = preferences(context).getString("history_${CacheIdentity.key(token)}", null) ?: return emptyList()
        val type = object : TypeToken<List<DuelInHistoryResponse>>() {}.type
        return runCatching { gson.fromJson<List<DuelInHistoryResponse>>(json, type) }.getOrDefault(emptyList())
    }

    fun storeHistory(context: Context, token: String, history: List<DuelInHistoryResponse>) {
        preferences(context).edit()
            .putString("history_${CacheIdentity.key(token)}", gson.toJson(history))
            .apply()
    }

    fun readStats(context: Context, token: String?): DuelStatsResponse? {
        token ?: return null
        val json = preferences(context).getString("stats_${CacheIdentity.key(token)}", null) ?: return null
        return runCatching { gson.fromJson(json, DuelStatsResponse::class.java) }.getOrNull()
    }

    fun storeStats(context: Context, token: String, stats: DuelStatsResponse) {
        preferences(context).edit()
            .putString("stats_${CacheIdentity.key(token)}", gson.toJson(stats))
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
