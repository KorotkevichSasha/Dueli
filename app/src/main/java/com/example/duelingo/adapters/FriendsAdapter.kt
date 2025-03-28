package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R
import com.example.duelingo.dto.response.FriendResponse
import de.hdodenhof.circleimageview.CircleImageView
import com.bumptech.glide.Glide
import com.example.duelingo.manager.AvatarManager

class FriendsAdapter(
    private val friends: List<FriendResponse>,
    private val avatarManager: AvatarManager
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: CircleImageView = view.findViewById(R.id.friendAvatar)
        val name: TextView = view.findViewById(R.id.friendName)
        val points: TextView = view.findViewById(R.id.pointsText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_item, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]

        holder.name.text = friend.username
        holder.points.text = "${friend.points} очк."

        avatarManager.loadAvatar(friend.id.toString(), holder.avatar)
    }

    override fun getItemCount() = friends.size
}