package com.example.duelingo.utils

import android.content.Context
import com.example.duelingo.dto.response.TestSummaryResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LearningCatalogCache {
    private const val PREFS = "duelrush_learning_catalog_cache"
    private val gson = Gson()

    fun read(context: Context, token: String?): List<TestSummaryResponse> {
        token ?: return emptyList()
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CacheIdentity.key(token), null) ?: return emptyList()
        val type = object : TypeToken<List<TestSummaryResponse>>() {}.type
        return runCatching {
            gson.fromJson<List<TestSummaryResponse>>(json, type)
        }.getOrDefault(emptyList())
    }

    fun store(context: Context, token: String, tests: List<TestSummaryResponse>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CacheIdentity.key(token), gson.toJson(tests))
            .apply()
    }
}
