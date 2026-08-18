package com.example.duelingo.utils

import android.content.Context
import com.example.duelingo.dto.response.UserProfileResponse
import com.google.gson.Gson

object ProfileCache {
    private const val PREFS = "duelrush_profile_cache"

    fun read(context: Context, token: String?): UserProfileResponse? {
        token ?: return null
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stableKey = CacheIdentity.key(token)
        val json = preferences.getString(stableKey, null)
            ?: preferences.getString(token.hashCode().toString(), null)?.also {
                preferences.edit().putString(stableKey, it).apply()
            }
            ?: return null
        return runCatching { Gson().fromJson(json, UserProfileResponse::class.java) }.getOrNull()
    }

    fun store(context: Context, token: String, profile: UserProfileResponse) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CacheIdentity.key(token), Gson().toJson(profile))
            .apply()
    }
}
