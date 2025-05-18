package com.example.duelingo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R
import com.example.duelingo.adapters.FriendRequestsAdapter
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch
import java.util.UUID

class FriendRequestsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tokenManager: TokenManager
    private lateinit var avatarManager: AvatarManager
    private lateinit var adapter: FriendRequestsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_friends_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.friends_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        tokenManager = TokenManager(requireContext())
        avatarManager = AvatarManager(requireContext(), tokenManager, requireContext().getSharedPreferences("user_prefs", 0))
        
        adapter = FriendRequestsAdapter(
            avatarManager = avatarManager,
            onAccept = { requestId -> updateRequestStatus(requestId, "accept") },
            onReject = { requestId -> updateRequestStatus(requestId, "reject") }
        )
        
        recyclerView.adapter = adapter
        loadRequests()
    }

    private fun loadRequests() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.relationshipService.getIncomingRequests(
                    "Bearer ${tokenManager.getAccessToken()}"
                )
                if (response.isSuccessful) {
                    response.body()?.let { requests ->
                        adapter.submitList(requests)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateRequestStatus(requestId: UUID, action: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.relationshipService.updateRelationshipStatus(
                    "Bearer ${tokenManager.getAccessToken()}",
                    requestId,
                    action
                )
                if (response.isSuccessful) {
                    loadRequests() // Reload the list after update
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    companion object {
        fun newInstance() = FriendRequestsFragment()
    }
} 