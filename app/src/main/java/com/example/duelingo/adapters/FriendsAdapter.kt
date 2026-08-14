package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R
import com.example.duelingo.dto.response.FriendResponse
import com.example.duelingo.manager.AvatarManager
import de.hdodenhof.circleimageview.CircleImageView

class FriendsAdapter(
    private val avatarManager: AvatarManager,
    private val onFriendClick: (FriendResponse) -> Unit
) : ListAdapter<FriendResponse, FriendsAdapter.FriendViewHolder>(DiffCallback) {

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: CircleImageView = view.findViewById(R.id.friendAvatar)
        val name: TextView = view.findViewById(R.id.friendName)
        val points: TextView = view.findViewById(R.id.pointsText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FriendViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.friend_item, parent, false)
    )

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = getItem(position)
        holder.name.text = friend.username
        holder.points.text = holder.itemView.context.getString(R.string.profile_points_format, friend.points)
        avatarManager.loadAvatar(friend.id.toString(), holder.avatar, friend.avatarUrl)
        holder.itemView.setOnClickListener { onFriendClick(friend) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FriendResponse>() {
        override fun areItemsTheSame(oldItem: FriendResponse, newItem: FriendResponse) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FriendResponse, newItem: FriendResponse) = oldItem == newItem
    }
}
