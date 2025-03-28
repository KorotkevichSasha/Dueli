package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.duelingo.R
import com.example.duelingo.dto.response.LeaderboardResponse
import com.example.duelingo.dto.response.PaginationResponse
import com.example.duelingo.dto.response.UserInLeaderboardResponse
import com.example.duelingo.manager.AvatarManager
import de.hdodenhof.circleimageview.CircleImageView

class LeaderboardAdapter(private var users: LeaderboardResponse, private val avatarManager: AvatarManager) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankText: TextView = view.findViewById(R.id.rankText)
        val avatarImage: CircleImageView = view.findViewById(R.id.avatarImage)
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val pointsText: TextView = view.findViewById(R.id.pointsText)
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
                holder.rankText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gray))
                holder.usernameText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gray))
                holder.pointsText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gray))
            }
        }
        holder.avatarImage.setImageResource(R.drawable.default_profile)

        avatarManager.loadAvatar(user.id, holder.avatarImage)

    }

    override fun getItemCount(): Int = users.top.content.size

    fun updateData(newUsers: LeaderboardResponse) {
        users = newUsers
        notifyDataSetChanged()
    }
}
