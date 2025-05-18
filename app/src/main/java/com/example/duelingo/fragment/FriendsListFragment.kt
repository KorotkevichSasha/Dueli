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
import com.example.duelingo.adapters.FriendsAdapter
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch

class FriendsListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tokenManager: TokenManager
    private lateinit var avatarManager: AvatarManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_friends_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.friends_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        tokenManager = TokenManager(requireContext())
        avatarManager = AvatarManager(requireContext(), tokenManager, requireContext().getSharedPreferences("user_prefs", 0))
        
        loadFriends()
    }

    private fun loadFriends() {
        lifecycleScope.launch {
            try {
                val friends = ApiClient.userService.getCurrentUserFriends("Bearer ${tokenManager.getAccessToken()}")
                    .distinctBy { it.id }
                
                recyclerView.adapter = FriendsAdapter(friends, avatarManager)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    companion object {
        fun newInstance() = FriendsListFragment()
    }
} 