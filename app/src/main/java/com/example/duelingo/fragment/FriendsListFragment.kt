package com.example.duelingo.fragment

import android.os.Bundle
import android.content.Intent
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
import com.example.duelingo.adapters.FriendsAdapter
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.duelingo.utils.RefreshEvents
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import com.example.duelingo.databinding.DialogFriendProfileBinding
import com.example.duelingo.databinding.DialogDuelDifficultyBinding
import com.example.duelingo.databinding.DialogConfirmActionBinding
import com.example.duelingo.databinding.DialogReportReasonBinding
import com.example.duelingo.dto.request.RelationshipRequest
import com.example.duelingo.dto.request.UserReportRequest
import com.example.duelingo.dto.request.DuelChallengeRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.duelingo.utils.AppToast as Toast
import com.example.duelingo.activity.MenuActivity

class FriendsListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tokenManager: TokenManager
    private lateinit var avatarManager: AvatarManager
    private lateinit var emptyState: View
    private lateinit var friendsAdapter: FriendsAdapter
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
        friendsAdapter = FriendsAdapter(avatarManager, ::showFriendProfile)
        recyclerView.adapter = friendsAdapter
        
        loadFriends()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    RefreshEvents.events.collect { event ->
                        if (event == RefreshEvents.DataSet.FRIENDS) loadFriends()
                    }
                }
                launch {
                    while (isActive) {
                        delay(15_000)
                        loadFriends()
                    }
                }
            }
        }
    }

    private fun loadFriends() {
        if (loading) return
        loading = true
        lifecycleScope.launch {
            try {
                val friends = ApiClient.userService.getCurrentUserFriends("Bearer ${tokenManager.getAccessToken()}")
                    .distinctBy { it.id }
                
                friendsAdapter.submitList(friends)
                recyclerView.visibility = if (friends.isEmpty()) View.GONE else View.VISIBLE
                emptyState.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                // Handle error
            } finally {
                loading = false
            }
        }
    }

    private fun showFriendProfile(friend: com.example.duelingo.dto.response.FriendResponse) {
        val content = DialogFriendProfileBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(content.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                val width = (resources.displayMetrics.widthPixels - 32 * resources.displayMetrics.density).toInt()
                    .coerceAtMost((480 * resources.displayMetrics.density).toInt())
                window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }
        content.usernameText.text = friend.username
        content.pointsText.text = getString(R.string.profile_points_format, friend.points)
        avatarManager.loadAvatar(friend.id.toString(), content.avatarImage, friend.avatarUrl)
        content.closeButton.setOnClickListener { dialog.dismiss() }
        content.challengeButton.setOnClickListener {
            showFriendDuelDifficulty(friend, dialog)
        }
        content.removeButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.remove_friend_confirm_title)
                .setMessage(getString(R.string.remove_friend_confirm_message, friend.username))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.remove_friend) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val response = ApiClient.relationshipService.removeFriend(
                            "Bearer ${tokenManager.getAccessToken()}", friend.id
                        )
                        if (response.isSuccessful) {
                            dialog.dismiss(); loadFriends()
                            RefreshEvents.notifyChanged(RefreshEvents.DataSet.FRIENDS)
                        }
                    }
                }
                .show()
        }
        content.reportButton.setOnClickListener { showReportReasons(friend, dialog) }
        content.blockButton.setOnClickListener {
            showBlockConfirmation(friend, dialog)
        }
        dialog.show()
    }

    private fun showFriendDuelDifficulty(
        friend: com.example.duelingo.dto.response.FriendResponse,
        profileDialog: Dialog
    ) {
        val content = DialogDuelDifficultyBinding.inflate(layoutInflater)
        val difficultyDialog = createStyledDialog(content.root, 520)
        content.titleText.setText(R.string.challenge_to_duel)
        content.subtitleText.setText(R.string.friend_duel_difficulty_subtitle)
        content.closeButton.setOnClickListener { difficultyDialog.dismiss() }

        fun challenge(level: String) {
            content.closeButton.isEnabled = false
            content.easyButton.isEnabled = false
            content.mediumButton.isEnabled = false
            content.hardButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching {
                    ApiClient.duelHistoryService.challengeFriend(
                        DuelChallengeRequest(friend.id.toString(), level)
                    )
                }.onSuccess {
                    difficultyDialog.dismiss()
                    profileDialog.dismiss()
                    Toast.makeText(requireContext(), R.string.duel_challenge_sent, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), MenuActivity::class.java))
                }.onFailure {
                    content.closeButton.isEnabled = true
                    content.easyButton.isEnabled = true
                    content.mediumButton.isEnabled = true
                    content.hardButton.isEnabled = true
                    Toast.makeText(requireContext(), R.string.duel_challenge_expired, Toast.LENGTH_SHORT).show()
                }
            }
        }

        content.easyButton.setOnClickListener { challenge("EASY") }
        content.mediumButton.setOnClickListener { challenge("MEDIUM") }
        content.hardButton.setOnClickListener { challenge("HARD") }
        difficultyDialog.show()
    }

    private fun showBlockConfirmation(
        friend: com.example.duelingo.dto.response.FriendResponse,
        profileDialog: Dialog
    ) {
        val content = DialogConfirmActionBinding.inflate(layoutInflater)
        val confirmationDialog = createStyledDialog(content.root, 440)
        content.titleText.setText(R.string.block_user_confirm_title)
        content.messageText.text = getString(R.string.block_user_confirm_message, friend.username)
        content.cancelButton.setOnClickListener { confirmationDialog.dismiss() }
        content.confirmButton.setOnClickListener {
            content.cancelButton.isEnabled = false
            content.confirmButton.isEnabled = false
            blockFriend(friend, profileDialog, confirmationDialog) {
                content.cancelButton.isEnabled = true
                content.confirmButton.isEnabled = true
            }
        }
        confirmationDialog.show()
    }

    private fun createStyledDialog(content: View, maxWidthDp: Int): Dialog =
        Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(content)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                val width = (resources.displayMetrics.widthPixels - 32 * resources.displayMetrics.density)
                    .toInt().coerceAtMost((maxWidthDp * resources.displayMetrics.density).toInt())
                window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }

    private fun showReportReasons(friend: com.example.duelingo.dto.response.FriendResponse, dialog: Dialog) {
        val labels = resources.getStringArray(R.array.report_reasons)
        val codes = arrayOf("INAPPROPRIATE_AVATAR", "OFFENSIVE_NAME", "HARASSMENT", "OTHER")
        val content = DialogReportReasonBinding.inflate(layoutInflater)
        val reportDialog = createStyledDialog(content.root, 440)
        val buttons = listOf(
            content.reasonAvatarButton,
            content.reasonNameButton,
            content.reasonHarassmentButton,
            content.reasonOtherButton
        )
        buttons.forEachIndexed { index, button ->
            button.text = labels[index]
            button.setOnClickListener {
                buttons.forEach { it.isEnabled = false }
                content.cancelButton.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        ApiClient.relationshipService.reportUser(
                            "Bearer ${tokenManager.getAccessToken()}",
                            UserReportRequest(friend.id, codes[index])
                        )
                    }.onSuccess { response ->
                        if (response.isSuccessful) {
                            reportDialog.dismiss()
                            dialog.dismiss()
                            Toast.makeText(requireContext(), R.string.report_sent, Toast.LENGTH_SHORT).show()
                        } else {
                            buttons.forEach { it.isEnabled = true }
                            content.cancelButton.isEnabled = true
                        }
                    }.onFailure {
                        buttons.forEach { it.isEnabled = true }
                        content.cancelButton.isEnabled = true
                    }
                }
            }
        }
        content.cancelButton.setOnClickListener { reportDialog.dismiss() }
        reportDialog.show()
    }

    private fun blockFriend(
        friend: com.example.duelingo.dto.response.FriendResponse,
        profileDialog: Dialog,
        confirmationDialog: Dialog,
        onFailure: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                ApiClient.relationshipService.blockUser(
                    "Bearer ${tokenManager.getAccessToken()}", RelationshipRequest(friend.id)
                )
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    confirmationDialog.dismiss()
                    profileDialog.dismiss()
                    loadFriends()
                    Toast.makeText(requireContext(), R.string.user_blocked, Toast.LENGTH_SHORT).show()
                } else {
                    onFailure()
                }
            }.onFailure { onFailure() }
        }
    }

    companion object {
        fun newInstance() = FriendsListFragment()
    }
}
