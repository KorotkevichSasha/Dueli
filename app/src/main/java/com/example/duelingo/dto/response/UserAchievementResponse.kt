package com.example.duelingo.dto.response

import java.util.UUID


class UserAchievementResponse(
    val achievementId: UUID,
    val title: String,
    val description: String,
    type: AchievementType,
    level: AchievementLevel,
    val requiredValue: Int,
    val currentValue: Int,
    val isAchieved: Boolean,
    val iconUrl: String?,
    val rewardGold: Int = 0,
    val rewardClaimed: Boolean = false
) {
    val type: AchievementType = type
    val level: AchievementLevel = level
}
