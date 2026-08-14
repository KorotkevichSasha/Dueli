package com.example.duelingo.manager

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {
    private const val PREFS = "locale_preferences"
    private const val KEY_LANGUAGE = "language"
    private const val DEFAULT_LANGUAGE = "ru"

    fun init(context: Context) {
        applyLanguage(getLanguage(context))
    }

    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT_LANGUAGE)
            ?: DEFAULT_LANGUAGE

    fun setLanguage(context: Context, languageTag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageTag)
            .apply()
        applyLanguage(languageTag)
    }

    private fun applyLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
