package com.example.duelingo.utils

import com.example.duelingo.BuildConfig

object AppConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL.trimEnd('/') + "/"

    const val CONNECT_TIMEOUT = 15L
    const val READ_TIMEOUT = 15L
    const val WRITE_TIMEOUT = 15L
}
