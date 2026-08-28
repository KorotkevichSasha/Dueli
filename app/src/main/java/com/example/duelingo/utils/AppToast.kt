package com.example.duelingo.utils

import android.content.Context
import androidx.annotation.StringRes

/** A new transient message replaces the current one instead of joining a Toast queue. */
object AppToast {
    const val LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT
    const val LENGTH_LONG = android.widget.Toast.LENGTH_LONG

    private var currentToast: android.widget.Toast? = null

    @Synchronized
    fun makeText(context: Context, text: CharSequence, duration: Int): android.widget.Toast {
        currentToast?.cancel()
        return android.widget.Toast.makeText(context.applicationContext, text, duration).also {
            currentToast = it
        }
    }

    fun makeText(context: Context, @StringRes textRes: Int, duration: Int): android.widget.Toast =
        makeText(context, context.getText(textRes), duration)
}
