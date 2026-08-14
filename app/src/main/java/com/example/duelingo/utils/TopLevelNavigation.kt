package com.example.duelingo.utils

import android.app.Activity
import android.content.Intent

/** Opens one clean top-level destination without accumulating duplicate tab Activities. */
fun Activity.openTopLevel(destination: Class<out Activity>) {
    if (javaClass == destination) return
    val intent = Intent(this, destination).addFlags(
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_NO_ANIMATION
    )
    startActivity(intent)
    finish()
}
