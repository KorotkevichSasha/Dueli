package com.example.duelingo.utils

import com.example.duelingo.BuildConfig

object AppConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL.trimEnd('/') + "/"

    // Render's free instances can take well over a minute to wake after inactivity.
    // Keep connection failures quick, but allow an already-connected cold start to finish.
    const val CONNECT_TIMEOUT = 20L
    const val READ_TIMEOUT = 135L
    const val WRITE_TIMEOUT = 30L
}
