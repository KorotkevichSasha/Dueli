package com.example.duelingo.utils

import android.content.Context
import com.example.duelingo.dto.response.EconomyResponse
import com.google.gson.Gson

object EconomyCache {
    private const val PREFS = "economy_cache"
    private const val DATA = "latest"
    private val gson = Gson()

    fun read(context: Context): EconomyResponse? = runCatching {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(DATA, null)
            ?.let { gson.fromJson(it, EconomyResponse::class.java) }
    }.getOrNull()

    fun store(context: Context, value: EconomyResponse) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(DATA, gson.toJson(value)).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
