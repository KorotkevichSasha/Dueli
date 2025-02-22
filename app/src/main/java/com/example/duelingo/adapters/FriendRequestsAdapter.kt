package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.duelingo.R
import com.example.duelingo.dto.response.RelationshipResponse
import com.example.duelingo.manager.AvatarManager
import java.util.UUID

class FriendRequestsAdapter(
    private val onAccept: (UUID) -> Unit,
    private val onReject: (UUID) -> Unit,
    private val avatarManager: AvatarManager
) : ListAdapter<RelationshipResponse, FriendRequestsAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.usernameText)
        val avatar: ImageView = view.findViewById(R.id.avatarImage)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnReject: Button = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.username.text = item.fromUsername

        avatarManager.loadAvatar(item.fromUserId.toString(), holder.avatar)

        holder.btnAccept.setOnClickListener { onAccept(item.id) }
        holder.btnReject.setOnClickListener { onReject(item.id) }
    }

    fun submitUpdatedList(list: List<RelationshipResponse>) {
        submitList(list)
    }

    class DiffCallback : DiffUtil.ItemCallback<RelationshipResponse>() {
        override fun areItemsTheSame(oldItem: RelationshipResponse, newItem: RelationshipResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RelationshipResponse, newItem: RelationshipResponse): Boolean {
            return oldItem == newItem
        }
    }
}