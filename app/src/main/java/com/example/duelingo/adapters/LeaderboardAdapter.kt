package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.example.duelingo.R
import com.example.duelingo.dto.response.LeaderboardResponse
import com.example.duelingo.dto.response.PaginationResponse
import com.example.duelingo.dto.response.UserInLeaderboardResponse
import com.example.duelingo.manager.AvatarManager
import com.google.android.material.color.MaterialColors
import de.hdodenhof.circleimageview.CircleImageView
import com.example.duelingo.utils.LeagueVisuals

class LeaderboardAdapter(
    private var users: LeaderboardResponse,
    private val avatarManager: AvatarManager,
    private val onUserClick: (UserInLeaderboardResponse) -> Unit
) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankText: TextView = view.findViewById(R.id.rankText)
        val avatarImage: CircleImageView = view.findViewById(R.id.avatarImage)
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val pointsText: TextView = view.findViewById(R.id.pointsText)
        val leagueIcon: ImageView = view.findViewById(R.id.rowLeagueIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users.top.content[position]
        holder.rankText.text = user.rank.toString()
        holder.usernameText.text = user.username
        holder.pointsText.text = user.points.toString()
        holder.leagueIcon.setImageResource(LeagueVisuals.forId(user.league?.id).icon)

        when (user.rank) {
            1L -> {
                holder.rankText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gold))
                holder.usernameText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gold))
                holder.pointsText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gold))
            }
            2L -> {
                holder.rankText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.silver))
                holder.usernameText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.silver))
                holder.pointsText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.silver))
            }
            3L -> {
                holder.rankText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.bronze))
                holder.usernameText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.bronze))
                holder.pointsText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.bronze))
            }
            else -> {
                val onSurface = MaterialColors.getColor(
                    holder.itemView,
                    com.google.android.material.R.attr.colorOnSurface
                )
                holder.rankText.setTextColor(onSurface)
                holder.usernameText.setTextColor(onSurface)
                holder.pointsText.setTextColor(onSurface)
            }
        }
        holder.avatarImage.setImageResource(R.drawable.default_profile)

        avatarManager.loadAvatar(user.id, holder.avatarImage)
        holder.itemView.setOnClickListener { onUserClick(user) }

    }

    override fun getItemCount(): Int = users.top.content.size

    fun updateData(newUsers: LeaderboardResponse) {
        if (users == newUsers) return
        val oldItems = users.top.content
        val newItems = newUsers.top.content
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldItems.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition].id == newItems[newItemPosition].id
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition] == newItems[newItemPosition]
        })
        users = newUsers
        diff.dispatchUpdatesTo(this)
    }
}
