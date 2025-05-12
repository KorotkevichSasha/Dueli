package com.example.duelingo.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.duelingo.R
import com.example.duelingo.dto.response.AchievementLevel
import com.example.duelingo.dto.response.UserAchievementResponse
import androidx.core.graphics.toColorInt

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

        @SuppressLint("SetTextI18n")
        fun bind(achievement: UserAchievementResponse) {
            tvTitle.text = achievement.title
            tvDescription.text = achievement.description
            tvProgressText.text = "${achievement.currentValue} / ${achievement.requiredValue}"

            progressBar.max = achievement.requiredValue
            progressBar.progress = achievement.currentValue

            val bgRes = when (achievement.level) {
                AchievementLevel.BRONZE -> R.drawable.ic_achievement_bronze
                AchievementLevel.SILVER -> R.drawable.ic_achievement_silver
                AchievementLevel.GOLD -> R.drawable.ic_achievement_gold
            }

            itemView.background = ContextCompat.getDrawable(itemView.context, bgRes)

            if (!achievement.isAchieved) {
                ivIcon.alpha = 0.3f
                ivStatus.setImageResource(R.drawable.ic_lock)
            } else {
                ivIcon.alpha = 1.0f
                ivStatus.setImageResource(R.drawable.img)
            }

            Glide.with(ivIcon.context)
                .load(achievement.iconUrl)
                .into(ivIcon)

//            ivStatus.setImageResource(
//                if (achievement.isAchieved) R.drawable.img
//                else R.drawable.ic_lock
//            )
        }
    }
}
