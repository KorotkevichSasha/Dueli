package com.example.duelingo.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import com.example.duelingo.R

/** Reuses the four main screens so their already-rendered content appears immediately. */
fun Activity.openTopLevel(destination: Class<out Activity>) {
    if (javaClass == destination) return
    val intent = Intent(this, destination).addFlags(
        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
    )
    val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in_fast, R.anim.fade_out_fast)
    startActivity(intent, options.toBundle())
}
