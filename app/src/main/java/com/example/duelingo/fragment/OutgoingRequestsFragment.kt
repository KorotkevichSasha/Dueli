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
import com.example.duelingo.adapters.OutgoingRequestsAdapter
import com.example.duelingo.databinding.FragmentFriendsListBinding
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID
import com.example.duelingo.utils.RefreshEvents

class OutgoingRequestsFragment : Fragment() {
    private var _binding: FragmentFriendsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private lateinit var adapter: OutgoingRequestsAdapter
    private var loading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentFriendsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        tokenManager = TokenManager(requireContext())
        val avatars = AvatarManager(requireContext(), tokenManager, requireContext().getSharedPreferences("user_prefs", 0))
        adapter = OutgoingRequestsAdapter(avatars, ::cancelRequest)
        binding.friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.friendsRecyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    RefreshEvents.events.collect { event ->
                        if (event == RefreshEvents.DataSet.FRIENDS) loadRequests()
                    }
                }
                launch {
                    while (isActive) {
                        delay(15_000)
                        loadRequests()
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); loadRequests() }

    private fun loadRequests() = viewLifecycleOwner.lifecycleScope.launch {
        if (loading) return@launch
        loading = true
        try {
            runCatching { ApiClient.relationshipService.getOutgoingRequests("Bearer ${tokenManager.getAccessToken()}") }
                .onSuccess { response -> if (response.isSuccessful) {
                    val items = response.body().orEmpty()
                    adapter.submitList(items)
                    binding.friendsRecyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                    binding.friendsEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }}
        } finally {
            loading = false
        }
    }

    private fun cancelRequest(id: UUID) = viewLifecycleOwner.lifecycleScope.launch {
        val response = ApiClient.relationshipService.cancelOutgoingRequest("Bearer ${tokenManager.getAccessToken()}", id)
        if (response.isSuccessful) loadRequests()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
    companion object { fun newInstance() = OutgoingRequestsFragment() }
}
