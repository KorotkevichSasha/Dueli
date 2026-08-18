package com.example.duelingo.storage

import android.content.Context
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class LearningHabitTracker(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Snapshot(val streakDays: Int, val minutesToday: Int)

    fun snapshot(): Snapshot {
        val today = localEpochDay()
        val recordedDay = preferences.getLong(KEY_DAY, Long.MIN_VALUE)
        return Snapshot(
            streakDays = preferences.getInt(KEY_STREAK, 0),
            minutesToday = if (recordedDay == today) preferences.getInt(KEY_MINUTES, 0) else 0
        )
    }

    fun recordPractice(minutes: Int = 5) {
        val today = localEpochDay()
        val previousDay = preferences.getLong(KEY_DAY, Long.MIN_VALUE)
        val previousStreak = preferences.getInt(KEY_STREAK, 0)
        val streak = when {
            previousDay == today -> previousStreak.coerceAtLeast(1)
            previousDay == today - 1 -> previousStreak.coerceAtLeast(1) + 1
            else -> 1
        }
        val minutesToday = if (previousDay == today) {
            preferences.getInt(KEY_MINUTES, 0) + minutes
        } else {
            minutes
        }
        preferences.edit()
            .putLong(KEY_DAY, today)
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_MINUTES, minutesToday)
            .apply()
    }

    companion object {
        private const val PREFS = "duelrush_learning_habit"
        private const val KEY_DAY = "practice_day"
        private const val KEY_STREAK = "streak_days"
        private const val KEY_MINUTES = "minutes_today"

        private fun localEpochDay(): Long {
            val now = System.currentTimeMillis()
            val localMillis = now + TimeZone.getDefault().getOffset(now)
            return TimeUnit.MILLISECONDS.toDays(localMillis)
        }
    }
}
