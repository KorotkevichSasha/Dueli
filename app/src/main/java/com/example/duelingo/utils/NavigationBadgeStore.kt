package com.example.duelingo.utils

import android.content.Context

object NavigationBadgeStore {
    private const val PREFS = "navigation_badges"
    private const val ACHIEVEMENTS = "claimable_achievements"
    private const val FRIEND_REQUESTS = "friend_requests"

    fun pendingProfileCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(ACHIEVEMENTS, 0) + prefs.getInt(FRIEND_REQUESTS, 0)
    }

    fun setClaimableAchievements(context: Context, count: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(ACHIEVEMENTS, count.coerceAtLeast(0)).apply()
    }

    fun setFriendRequests(context: Context, count: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(FRIEND_REQUESTS, count.coerceAtLeast(0)).apply()
    }
}
