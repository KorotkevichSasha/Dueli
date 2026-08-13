package com.example.duelingo.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Lightweight in-process invalidation for screens that display mutable server data. */
object RefreshEvents {
    enum class DataSet {
        FRIENDS,
        DUEL_HISTORY,
        LEADERBOARD,
        PROFILE,
        LEARNING
    }

    private val mutableEvents = MutableSharedFlow<DataSet>(extraBufferCapacity = 16)
    val events = mutableEvents.asSharedFlow()

    fun notifyChanged(dataSet: DataSet) {
        mutableEvents.tryEmit(dataSet)
    }
}
