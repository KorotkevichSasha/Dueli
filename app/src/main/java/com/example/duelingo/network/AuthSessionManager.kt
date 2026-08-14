package com.example.duelingo.network

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.storage.TokenManager
import java.util.concurrent.atomic.AtomicBoolean

object AuthSessionManager {
    private lateinit var applicationContext: Context
    private val redirectInProgress = AtomicBoolean(false)

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun onAuthenticated() {
        redirectInProgress.set(false)
    }

    fun expireSession() {
        if (!::applicationContext.isInitialized) return

        TokenManager(applicationContext).clearTokens()
        if (!redirectInProgress.compareAndSet(false, true)) return

        Handler(Looper.getMainLooper()).post {
            applicationContext.startActivity(
                Intent(applicationContext, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true)
                }
            )
        }
    }
}
