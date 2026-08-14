package com.example.duelingo.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import com.example.duelingo.R

/** Opens one clean top-level destination without accumulating duplicate tab Activities. */
fun Activity.openTopLevel(destination: Class<out Activity>) {
    if (javaClass == destination) return
    val intent = Intent(this, destination).addFlags(
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    )
    val transition = ActivityOptions.makeCustomAnimation(
        this,
        R.anim.top_level_enter,
        R.anim.top_level_exit
    )
    startActivity(intent, transition.toBundle())
    finish()
    @Suppress("DEPRECATION")
    overridePendingTransition(R.anim.top_level_enter, R.anim.top_level_exit)
}
