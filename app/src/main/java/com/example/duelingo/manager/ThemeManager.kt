package com.example.duelingo.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF_NAME = "theme_preferences"
    private const val KEY_IS_DARK_MODE = "is_dark_mode"
    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Применяем сохраненную тему при запуске
        applyTheme(isDarkMode())
    }

    fun isDarkMode(): Boolean {
        return preferences.getBoolean(KEY_IS_DARK_MODE, true)
    }

    fun setDarkMode(isDark: Boolean) {
        preferences.edit().putBoolean(KEY_IS_DARK_MODE, isDark).apply()
        applyTheme(isDark)
    }

    private fun applyTheme(isDark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
} 