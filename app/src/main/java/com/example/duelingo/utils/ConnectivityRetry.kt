package com.example.duelingo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class ConnectivityRetry(
    context: Context,
    lifecycle: Lifecycle,
    private val retry: () -> Unit
) : DefaultLifecycleObserver {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var registered = false
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mainHandler.postDelayed(retry, 350L)
        }
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (registered) return
        runCatching { manager.registerDefaultNetworkCallback(callback) }
            .onSuccess { registered = true }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!registered) return
        runCatching { manager.unregisterNetworkCallback(callback) }
        registered = false
        mainHandler.removeCallbacksAndMessages(null)
    }
}
