package com.example.duelingo.storage

import android.content.Context
import com.example.duelingo.dto.response.DuelInHistoryResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class OfflineDuelHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<DuelInHistoryResponse> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<DuelInHistoryResponse>>(
                raw,
                object : TypeToken<List<DuelInHistoryResponse>>() {}.type
            )
        }.getOrDefault(emptyList())
    }

    fun add(duel: DuelInHistoryResponse) {
        val updated = (listOf(duel) + getAll().filterNot { it.id == duel.id }).take(MAX_ITEMS)
        preferences.edit().putString(KEY_HISTORY, gson.toJson(updated)).apply()
    }

    companion object {
        private const val PREFERENCES = "offline_duel_history"
        private const val KEY_HISTORY = "duels"
        private const val MAX_ITEMS = 30
    }
}
