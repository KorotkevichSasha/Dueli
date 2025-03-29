package com.example.duelingo.utils

object AppConfig {
    const val BASE_URL = "http://192.168.0.101:8082"

    const val CONNECT_TIMEOUT = 15L
    const val READ_TIMEOUT = 15L
    const val WRITE_TIMEOUT = 15L

    object PrefsKeys {
        const val AUTH_TOKEN = "auth_token"
        const val USER_ID = "user_id"
    }
}