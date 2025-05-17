package com.example.duelingo

import android.app.Application
import com.example.duelingo.manager.ThemeManager

class DuelingoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализируем менеджер темы
        ThemeManager.init(this)
    }
} 