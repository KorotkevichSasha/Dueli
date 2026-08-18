package com.example.duelingo.storage

import android.content.Context

class OnboardingPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val isComplete: Boolean
        get() = preferences.getBoolean(KEY_COMPLETE, false)

    val learnerLevel: String
        get() = preferences.getString(KEY_LEVEL, LEVEL_BEGINNER) ?: LEVEL_BEGINNER

    val dailyGoalMinutes: Int
        get() = preferences.getInt(KEY_DAILY_GOAL, 10)

    fun complete(level: String, dailyGoalMinutes: Int) {
        preferences.edit()
            .putBoolean(KEY_COMPLETE, true)
            .putString(KEY_LEVEL, level)
            .putInt(KEY_DAILY_GOAL, dailyGoalMinutes)
            .apply()
    }

    companion object {
        private const val PREFS = "duelrush_onboarding"
        private const val KEY_COMPLETE = "complete"
        private const val KEY_LEVEL = "learner_level"
        private const val KEY_DAILY_GOAL = "daily_goal_minutes"

        const val LEVEL_BEGINNER = "beginner"
        const val LEVEL_INTERMEDIATE = "intermediate"
        const val LEVEL_ADVANCED = "advanced"
    }
}
