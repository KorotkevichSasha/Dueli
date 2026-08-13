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

class AchievementsAdapter(
    private var achievements: List<UserAchievementResponse>
) : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        holder.bind(achievement)
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

        @SuppressLint("SetTextI18n")
        fun bind(achievement: UserAchievementResponse) {
            tvTitle.text = achievement.title
            tvDescription.text = achievement.description
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
        }
    }
}
