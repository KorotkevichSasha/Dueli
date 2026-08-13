package com.example.duelingo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.databinding.ItemOutgoingRequestBinding
import com.example.duelingo.dto.response.RelationshipResponse
import com.example.duelingo.manager.AvatarManager
import java.util.UUID

class OutgoingRequestsAdapter(
    private val avatarManager: AvatarManager,
    private val onCancel: (UUID) -> Unit
) : ListAdapter<RelationshipResponse, OutgoingRequestsAdapter.ViewHolder>(Diff()) {
    class ViewHolder(val binding: ItemOutgoingRequestBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemOutgoingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val request = getItem(position)
        usernameText.text = request.toUsername
        avatarManager.loadAvatar(request.toUserId.toString(), avatarImage, request.toAvatarUrl)
        cancelButton.setOnClickListener { onCancel(request.id) }
    }
    class Diff : DiffUtil.ItemCallback<RelationshipResponse>() {
        override fun areItemsTheSame(oldItem: RelationshipResponse, newItem: RelationshipResponse) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RelationshipResponse, newItem: RelationshipResponse) = oldItem == newItem
    }
}
