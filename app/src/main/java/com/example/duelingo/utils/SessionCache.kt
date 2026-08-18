package com.example.duelingo.utils

import android.content.Context

/** Removes account-specific snapshots when a session is explicitly cleared. */
object SessionCache {
    private val preferenceFiles = listOf(
        "duelrush_profile_cache",
        "duelrush_duel_cache",
        "duelrush_learning_catalog_cache",
        "duelrush_leaderboard_cache",
        "offline_duel_history",
        "duelrush_learning_habit"
    )

    fun clear(context: Context) {
        preferenceFiles.forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }
        LeaderboardCache.clearMemory()
    }
}
