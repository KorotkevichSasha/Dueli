package com.example.duelingo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duelingo.R
import com.example.duelingo.adapters.FriendRequestsAdapter
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.duelingo.utils.RefreshEvents
import com.example.duelingo.utils.NavigationBadgeStore
import com.example.duelingo.utils.BottomNavigationController
import java.util.UUID

class FriendRequestsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tokenManager: TokenManager
    private lateinit var avatarManager: AvatarManager
    private lateinit var adapter: FriendRequestsAdapter
    private lateinit var emptyState: View
    private var loading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_friends_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.friends_recycler_view)
        emptyState = view.findViewById(R.id.friends_empty_state)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null
        
        tokenManager = TokenManager(requireContext())
        avatarManager = AvatarManager(requireContext(), tokenManager, requireContext().getSharedPreferences("user_prefs", 0))
        
        adapter = FriendRequestsAdapter(
            avatarManager = avatarManager,
            onAccept = { requestId -> updateRequestStatus(requestId, "accept") },
            onReject = { requestId -> updateRequestStatus(requestId, "reject") }
        )
        
        recyclerView.adapter = adapter
        loadRequests()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(15_000)
                    loadRequests()
                }
            }
        }
    }

    private fun loadRequests() {
        if (loading) return
        loading = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.relationshipService.getIncomingRequests(
                    "Bearer ${tokenManager.getAccessToken()}"
                )
                if (response.isSuccessful) {
                    response.body()?.let { requests ->
                        NavigationBadgeStore.setFriendRequests(requireContext(), requests.size)
                        activity?.let(BottomNavigationController::sync)
                        adapter.submitList(requests)
                        recyclerView.visibility = if (requests.isEmpty()) View.GONE else View.VISIBLE
                        emptyState.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                loading = false
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
                    RefreshEvents.notifyChanged(RefreshEvents.DataSet.FRIENDS)
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
