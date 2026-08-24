package com.example.duelingo.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R
import com.example.duelingo.dto.response.AchievementLevel
import com.example.duelingo.dto.response.AchievementType
import com.example.duelingo.dto.response.UserAchievementResponse
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton

class AchievementsAdapter(
    private var achievements: List<UserAchievementResponse>,
    private val onClaimReward: (UserAchievementResponse) -> Unit
) : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        holder.bind(achievement, onClaimReward)
    }

    override fun getItemCount(): Int = achievements.size

    fun updateData(newAchievements: List<UserAchievementResponse>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.achievementTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.achievementDescription)
        private val tvProgressText: TextView = itemView.findViewById(R.id.achievementProgressText)
        private val ivIcon: ImageView = itemView.findViewById(R.id.achievementIcon)
        private val ivStatus: ImageView = itemView.findViewById(R.id.achievementStatus)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.achievementProgressBar)
        private val card: MaterialCardView = itemView.findViewById(R.id.achievementCard)
        private val rewardButton: MaterialButton = itemView.findViewById(R.id.achievementRewardButton)

        @SuppressLint("SetTextI18n")
        fun bind(achievement: UserAchievementResponse, onClaimReward: (UserAchievementResponse) -> Unit) {
            val english = itemView.resources.configuration.locales[0].language == "en"
            tvTitle.text = if (english) englishTitle(achievement) else achievement.title
            tvDescription.text = if (english) englishDescription(achievement) else achievement.description
            tvProgressText.text = "${achievement.currentValue} / ${achievement.requiredValue}"

            progressBar.max = achievement.requiredValue.coerceAtLeast(1)
            progressBar.progress = achievement.currentValue.coerceIn(0, progressBar.max)

            val accentColor = ContextCompat.getColor(itemView.context, when (achievement.level) {
                AchievementLevel.BRONZE -> R.color.bronze
                AchievementLevel.SILVER -> R.color.silver
                AchievementLevel.GOLD -> R.color.gold
            })
            card.strokeColor = ColorUtils.setAlphaComponent(
                accentColor,
                if (achievement.isAchieved) 210 else 90
            )
            card.strokeWidth = if (achievement.isAchieved) 2 else 1
            progressBar.progressTintList = ColorStateList.valueOf(accentColor)

            ivIcon.setImageResource(when (achievement.type) {
                AchievementType.DUELS -> R.drawable.swords24
                AchievementType.FRIENDS -> R.drawable.ic_add_friend
                AchievementType.INVITES -> R.drawable.ic_invite
                AchievementType.TESTS -> R.drawable.graduation24
                AchievementType.WORDS -> R.drawable.add
            })
            ImageViewCompat.setImageTintList(ivIcon, ColorStateList.valueOf(accentColor))

            if (achievement.isAchieved) {
                ivIcon.alpha = 1f
                ivStatus.setImageResource(R.drawable.img)
                ImageViewCompat.setImageTintList(ivStatus, null)
            } else {
                ivIcon.alpha = 0.45f
                ivStatus.setImageResource(R.drawable.ic_lock)
                ImageViewCompat.setImageTintList(
                    ivStatus,
                    ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.gray))
                )
            }

            rewardButton.text = when {
                achievement.rewardClaimed -> itemView.context.getString(
                    R.string.achievement_reward_claimed, achievement.rewardGold)
                achievement.isAchieved -> itemView.context.getString(
                    R.string.achievement_reward_claim, achievement.rewardGold)
                else -> itemView.context.getString(
                    R.string.achievement_reward, achievement.rewardGold)
            }
            rewardButton.isEnabled = achievement.isAchieved && !achievement.rewardClaimed
            rewardButton.alpha = if (rewardButton.isEnabled) 1f else 0.68f
            rewardButton.setOnClickListener {
                if (achievement.isAchieved && !achievement.rewardClaimed) onClaimReward(achievement)
            }
        }

        private fun englishTitle(achievement: UserAchievementResponse): String = when (achievement.type) {
            AchievementType.DUELS -> when (achievement.requiredValue) {
                1 -> "First challenge"
                in 2..25 -> "Arena warm-up"
                in 26..100 -> "Seasoned duelist"
                else -> "DuelRush legend"
            }
            AchievementType.FRIENDS -> when (achievement.requiredValue) {
                1 -> "First teammate"
                in 2..10 -> "Learning circle"
                in 11..35 -> "Language club"
                else -> "Community leader"
            }
            AchievementType.INVITES -> when (achievement.requiredValue) {
                1 -> "First invite"
                2 -> "Duo assembled"
                in 3..5 -> "Your own squad"
                in 6..10 -> "Club captain"
                else -> "Community founder"
            }
            AchievementType.TESTS -> when (achievement.requiredValue) {
                1 -> "First ten"
                in 2..10 -> "Accurate learner"
                in 11..35 -> "Grammar navigator"
                else -> "Language architect"
            }
            AchievementType.WORDS -> when (achievement.requiredValue) {
                1 -> "First word"
                in 2..25 -> "Pocket dictionary"
                in 26..200 -> "Meaning collector"
                else -> "Living encyclopedia"
            }
        }

        private fun englishDescription(achievement: UserAchievementResponse): String = when (achievement.type) {
            AchievementType.DUELS -> itemView.context.getString(R.string.achievement_duels_description, achievement.requiredValue)
            AchievementType.FRIENDS -> itemView.context.getString(R.string.achievement_friends_description, achievement.requiredValue)
            AchievementType.INVITES -> itemView.context.resources.getQuantityString(
                R.plurals.achievement_invites_description,
                achievement.requiredValue,
                achievement.requiredValue
            )
            AchievementType.TESTS -> itemView.context.getString(R.string.achievement_tests_description, achievement.requiredValue)
            AchievementType.WORDS -> itemView.context.getString(R.string.achievement_words_description, achievement.requiredValue)
        }
    }
}
