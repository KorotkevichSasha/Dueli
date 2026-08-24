package com.example.duelingo.activity
import android.animation.Animator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.GridLayout
import androidx.core.widget.NestedScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.example.duelingo.R
import com.example.duelingo.BuildConfig
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.adapters.FriendRequestsAdapter
import com.example.duelingo.adapters.FriendsAdapter
import com.example.duelingo.databinding.ActivityProfileBinding
import com.example.duelingo.databinding.DialogAvatarPickerBinding
import com.example.duelingo.dto.request.RelationshipRequest
import com.example.duelingo.dto.response.FriendResponse
import com.example.duelingo.dto.response.UserProfileResponse
import com.example.duelingo.fragment.FriendRequestsFragment
import com.example.duelingo.fragment.FriendsListFragment
import com.example.duelingo.fragment.OutgoingRequestsFragment
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.utils.KeyboardInsets
import com.example.duelingo.utils.openTopLevel
import com.example.duelingo.manager.LocaleManager
import com.example.duelingo.manager.ThemeManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.UserService
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.RefreshEvents
import com.example.duelingo.utils.UserMessage
import com.example.duelingo.utils.LeagueVisuals
import com.example.duelingo.utils.ProfileCache
import com.example.duelingo.utils.ConnectivityRetry
import com.example.duelingo.utils.LeaderboardCache
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null
    private lateinit var tokenManager: TokenManager
    private lateinit var userService: UserService
    private lateinit var avatarManager: AvatarManager
    private var currentProfilePoints: Int = 0
    private var currentProfileUsername: String = ""
    private var currentProfileId: UUID? = null
    private val sharedPreferences by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }
    private var profileLoading = false
    private var offlineSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        userService = ApiClient.userService
        
        // Check if user is logged in
        if (tokenManager.getAccessToken().isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ConnectivityRetry(this, lifecycle) { loadProfile() }

        binding.profileIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.profileTest.setTextColor(Color.parseColor("#FF00A5FE"))

        avatarManager = AvatarManager(this, tokenManager, sharedPreferences)
        ProfileCache.read(this, tokenManager.getAccessToken())?.let(::updateUI)
        binding.profileImage.setOnClickListener { showAvatarPicker() }
        binding.uploadPhotoButton.setOnClickListener { showAvatarPicker() }
        binding.shareProfileButton.setOnClickListener { shareProfile() }

        binding.achievementsButton.setOnClickListener{ startActivity(Intent(this@ProfileActivity, AchievementActivity::class.java)) }

        setupFriendsSection()

        binding.tests.setOnClickListener {
            resetAll()
            openTopLevel(LearningActivity::class.java)
            changeColorAndIcon(binding.testIcon, binding.testTest, R.drawable.grad)
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
        }
        binding.duel.setOnClickListener {
            resetAll()
            openTopLevel(MenuActivity::class.java)
            changeColorAndIcon(binding.mainIcon, binding.mainTest, R.drawable.swo)
            playAnimation(binding.duelAnimation, binding.mainIcon, binding.mainTest, "swordAnim.json")
        }
        binding.leaderboard.setOnClickListener {
            resetAll()
            openTopLevel(RankActivity::class.java)
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }
        binding.profile.setOnClickListener {}

        binding.settingsButton.setOnClickListener {
            showThemeDialog()
        }
    }

    private fun setupFriendsSection() {
        // Setup ViewPager
        val pagerAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> FriendsListFragment.newInstance()
                    1 -> FriendRequestsFragment.newInstance()
                    2 -> OutgoingRequestsFragment.newInstance()
                    else -> throw IllegalArgumentException("Invalid position $position")
                }
            }
        }
        
        binding.friendsPager.adapter = pagerAdapter

        // Setup TabLayout
        TabLayoutMediator(binding.friendsTabs, binding.friendsPager) { tab, position ->
            // Inflate custom tab layout
            val customTab = layoutInflater.inflate(R.layout.custom_tab_layout, null) as RelativeLayout
            val tabText = customTab.findViewById<TextView>(R.id.tab_text)
            val notificationDot = customTab.findViewById<View>(R.id.notification_dot)
            
            // Set tab text
            tabText.text = when (position) {
                0 -> getString(R.string.friends)
                1 -> getString(R.string.incoming_requests_tab)
                2 -> getString(R.string.outgoing_requests_tab)
                else -> ""
            }
            
            // Show notification dot only for requests tab
            if (position == 1) {
                checkFriendRequests(notificationDot)
            }
            
            tab.customView = customTab
        }.attach()

        // Setup add friend button
        binding.addFriendButton.setOnClickListener {
            showAddFriendDialog()
        }

        // Update notification dot when page changes
        binding.friendsPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 1) {
                    // Hide notification dot when requests tab is selected
                    binding.friendsTabs.getTabAt(1)?.customView?.findViewById<View>(R.id.notification_dot)?.visibility = View.GONE
                }
            }
        })
    }

    private fun checkFriendRequests(notificationDot: View) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.relationshipService.getIncomingRequests(
                    "Bearer ${tokenManager.getAccessToken()}"
                )
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    notificationDot.visibility = View.VISIBLE
                } else {
                    notificationDot.visibility = View.GONE
                }
            } catch (e: Exception) {
                notificationDot.visibility = View.GONE
            }
        }
    }

    private fun showThemeDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_theme_settings)
        
        dialog.window?.apply {
            setGravity(Gravity.END or Gravity.TOP)
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            attributes?.y = 80
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val themeSwitch = dialog.findViewById<SwitchCompat>(R.id.theme_switch)
        themeSwitch.isChecked = ThemeManager.isDarkMode()
        
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkMode(isChecked)
        }

        val languageValue = dialog.findViewById<TextView>(R.id.language_value)
        languageValue.text = if (LocaleManager.getLanguage(this) == "en") {
            getString(R.string.language_english)
        } else {
            getString(R.string.language_russian)
        }
        dialog.findViewById<LinearLayout>(R.id.language_button).setOnClickListener {
            val current = if (LocaleManager.getLanguage(this) == "en") 1 else 0
            AlertDialog.Builder(this)
                .setTitle(R.string.language_title)
                .setSingleChoiceItems(
                    arrayOf(
                        getString(R.string.language_russian),
                        getString(R.string.language_english)
                    ),
                    current
                ) { picker, selected ->
                    dialog.dismiss()
                    picker.dismiss()
                    LocaleManager.setLanguage(this, if (selected == 1) "en" else "ru")
                }
                .show()
        }

        val logoutButton = dialog.findViewById<LinearLayout>(R.id.logout_button)
        logoutButton.setOnClickListener {
            dialog.dismiss()
            logout()
        }

        dialog.findViewById<LinearLayout>(R.id.delete_account_button).setOnClickListener {
            dialog.dismiss()
            confirmAccountDeletion()
        }
        dialog.findViewById<LinearLayout>(R.id.privacy_button).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)))
        }

        dialog.show()
    }

    private fun updateUI(response: UserProfileResponse) {
        currentProfilePoints = response.points
        currentProfileUsername = response.username
        currentProfileId = runCatching { UUID.fromString(response.id) }.getOrNull()
        binding.playerName.text = response.username
        binding.playerEmail.text = response.email
        binding.pointCount.text = getString(R.string.profile_points_format, response.points)
        binding.profilePointsValue.text = response.points.toString()
        val cachedGap = LeaderboardCache.current(this, tokenManager.getAccessToken())
            ?.currentUser?.pointsToNextRank
        binding.profileGoalValue.text = cachedGap?.toString() ?: "—"
        response.economy?.let { economy ->
            binding.profileGoldValue.text = getString(R.string.gold_balance, economy.gold)
            binding.profileRushValue.text = getString(
                R.string.rush_sparks_balance, economy.rushCharges, economy.maxRushCharges)
            val visual = LeagueVisuals.forId(economy.league.id)
            binding.profileLeagueIcon.setImageResource(visual.icon)
            binding.profileLeagueName.text = getString(R.string.league_title_format, getString(visual.name))
        }
        avatarManager.loadAvatar(response.id, binding.profileImage, response.avatarUrl)
    }

    private fun showAvatarPicker() {
        val content = DialogAvatarPickerBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(content.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                val width = (resources.displayMetrics.widthPixels - 24 * resources.displayMetrics.density).toInt()
                    .coerceAtMost((520 * resources.displayMetrics.density).toInt())
                window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }
        val avatarSize = (44 * resources.displayMetrics.density).toInt()
        (1..10).forEach { index ->
            val image = de.hdodenhof.circleimageview.CircleImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = avatarSize; height = avatarSize
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setImageResource(avatarManager.avatarResource(index))
                contentDescription = getString(R.string.default_avatar_number, index)
                isClickable = true
                isFocusable = true
                foreground = ContextCompat.getDrawable(this@ProfileActivity, android.R.drawable.list_selector_background)
                setOnClickListener {
                    avatarManager.selectDefaultAvatar(index, { profile ->
                        updateUI(profile); dialog.dismiss()
                    }, ::showToast)
                }
            }
            content.avatarGrid.addView(image)
        }
        content.closeButton.setOnClickListener { dialog.dismiss() }
        content.contentPolicyCheck.setOnCheckedChangeListener { _, checked ->
            content.uploadButton.isEnabled = checked
        }
        content.uploadButton.setOnClickListener { dialog.dismiss(); getContent.launch("image/*") }
        dialog.show()
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            avatarManager.uploadImage(it,
                onSuccess = { response ->
                    updateUI(response)
                },
                onError = { message ->
                    showToast(message)
                }
            )
        }
    }

    private fun loadProfile() {
        if (profileLoading) return
        profileLoading = true
        val accessToken = tokenManager.getAccessToken() ?: run {
            profileLoading = false
            showToast(getString(R.string.session_expired))
            return
        }

        val tokenWithBearer = "Bearer $accessToken"

        lifecycleScope.launch {
            try {
                val response = ApiClient.userService.getProfile(tokenWithBearer)
                withContext(Dispatchers.Main) {
                    updateUI(response)
                    ProfileCache.store(this@ProfileActivity, accessToken, response)
                    offlineSnackbar?.dismiss()
                    offlineSnackbar = null
                }
                loadProfileRank(tokenWithBearer)
            } catch (e: Exception) {
                Log.e("ProfileError", "Error loading profile: ${e.message}")
                offlineSnackbar?.dismiss()
                offlineSnackbar = Snackbar.make(
                    binding.root,
                    if (ProfileCache.read(this@ProfileActivity, accessToken) != null) {
                        UserMessage.from(this@ProfileActivity, e)
                    } else {
                        UserMessage.from(this@ProfileActivity, e)
                    },
                    Snackbar.LENGTH_LONG
                )
                    .setAnchorView(binding.bottomNavigation)
                    .setAction(R.string.retry_connection) { loadProfile() }
                offlineSnackbar?.show()
            } finally {
                profileLoading = false
            }
        }
    }

    private fun logout() {
        tokenManager.clearTokens()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private suspend fun loadProfileRank(authHeader: String) {
        val accessToken = authHeader.removePrefix("Bearer ")
        runCatching { ApiClient.leaderboardService.getLeaderboard(authHeader) }
            .onSuccess { leaderboard ->
                LeaderboardCache.store(this, accessToken, leaderboard)
                binding.profileRankValue.text = "#${leaderboard.currentUser.rank}"
                binding.profileGoalValue.text =
                    (leaderboard.currentUser.pointsToNextRank ?: 0).toString()
            }
            .onFailure {
                LeaderboardCache.current(this, accessToken)?.let { cached ->
                    binding.profileRankValue.text = "#${cached.currentUser.rank}"
                    binding.profileGoalValue.text =
                        (cached.currentUser.pointsToNextRank ?: 0).toString()
                }
                Log.w("ProfileActivity", "Could not load profile rank", it)
            }
    }

    private fun shareProfile() {
        val text = getString(R.string.share_profile_text, currentProfilePoints)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, currentProfileUsername.ifBlank { getString(R.string.app_name) })
            putExtra(Intent.EXTRA_TEXT, text)
        }, getString(R.string.share_profile)))
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadProfile()
    }

    private fun confirmAccountDeletion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_account)
            .setMessage(R.string.delete_account_confirmation)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteAccount() }
            .show()
    }

    private fun deleteAccount() {
        val accessToken = tokenManager.getAccessToken() ?: return logout()
        lifecycleScope.launch {
            try {
                userService.deleteAccount("Bearer $accessToken")
                sharedPreferences.edit().clear().apply()
                logout()
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Account deletion failed", e)
                showToast(getString(R.string.delete_account_failed))
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun changeColorAndIcon(icon: ImageView, text: TextView, iconRes: Int) {
        text.setTextColor(ContextCompat.getColor(this, R.color.blue_primary))
        icon.setColorFilter(ContextCompat.getColor(this, R.color.blue_primary))
        icon.setImageResource(iconRes)
    }

    private fun playAnimation(animationView: LottieAnimationView, icon: ImageView, text: TextView, animationFile: String) {
        currentAnimationView?.apply {
            cancelAnimation()
            visibility = View.GONE
        }

        currentIcon?.setColorFilter(Color.parseColor("#7A7A7B"))
        currentText?.setTextColor(Color.parseColor("#7A7A7B"))
        currentIcon?.visibility = View.VISIBLE

        currentAnimationView = animationView
        currentIcon = icon
        currentText = text

        icon.visibility = View.GONE

        animationView.visibility = View.VISIBLE
        animationView.setAnimation(animationFile)
        animationView.playAnimation()

        animationView.removeAllAnimatorListeners()
        animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animator) {}
        })

    }

    private fun showAddFriendDialog() {
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setContentView(R.layout.dialog_add_friend)

        val rootView = dialog.findViewById<ViewGroup>(android.R.id.content)
        if (rootView == null) {
            Log.e("ProfileActivity", "Failed to load add-friend dialog layout")
            showToast(getString(R.string.error_generic))
            return
        }

        val editUsername = rootView.findViewById<EditText>(R.id.editUsername)
        val btnSearch = rootView.findViewById<Button>(R.id.btnSearch)
        val progressBar = rootView.findViewById<ProgressBar>(R.id.progressBar)
        val userContainer = rootView.findViewById<LinearLayout>(R.id.userContainer)
        val resultsScroll = rootView.findViewById<NestedScrollView>(R.id.resultsScroll)
        val emptySearchText = rootView.findViewById<TextView>(R.id.emptySearchText)
        rootView.findViewById<View>(R.id.closeButton)?.setOnClickListener { dialog.dismiss() }

        if (editUsername == null || btnSearch == null || progressBar == null || userContainer == null) {
            Log.e("ProfileActivity", "Add-friend dialog has missing required views")
            showToast(getString(R.string.error_generic))
            return
        }

        btnSearch.setOnClickListener {
            val username = editUsername.text.toString()
            if (username.isNotEmpty()) {
                searchUser(username.trim(), progressBar, userContainer, resultsScroll, emptySearchText)
            } else {
                showToast(getString(R.string.error_username_required))
            }
        }

        dialog.setOnShowListener {
            dialog.findViewById<View>(android.R.id.content)?.let(KeyboardInsets::apply)
            val width = (resources.displayMetrics.widthPixels - 32 * resources.displayMetrics.density).toInt()
                .coerceAtMost((560 * resources.displayMetrics.density).toInt())
            dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        editUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                btnSearch.performClick()
                true
            } else false
        }
        dialog.show()
    }

    private fun searchUser(
        username: String,
        progressBar: ProgressBar,
        container: LinearLayout,
        resultsScroll: View,
        emptySearchText: TextView
    ) {
        if (!::tokenManager.isInitialized) {
            Log.e("ProfileActivity", "Token manager is not initialized")
            showToast(getString(R.string.session_expired))
            return
        }

        val accessToken = tokenManager.getAccessToken()
        if (accessToken == null) {
            showToast(getString(R.string.session_expired))
            return
        }

        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                container.removeAllViews()
                resultsScroll.visibility = View.GONE
                emptySearchText.visibility = View.GONE

                val response = ApiClient.userService.searchUsers(
                    "Bearer $accessToken",
                    username
                )

                val users = response.content.filterNot { it.id == currentProfileId }
                if (users.isNotEmpty()) {
                    users.forEach { user ->
                        showUserInfo(user, container)
                    }
                    resultsScroll.visibility = View.VISIBLE
                } else {
                    emptySearchText.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Friend search failed", e)
                showToast(UserMessage.from(this@ProfileActivity, e))
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showUserInfo(user: FriendResponse, container: LinearLayout) {
        val view = layoutInflater.inflate(R.layout.item_user, container, false)

        val usernameText = view.findViewById<TextView>(R.id.usernameText)
        val avatarImage = view.findViewById<ImageView>(R.id.avatarImage)
        val btnSendRequest = view.findViewById<Button>(R.id.btnSendRequest)
        val pointsText = view.findViewById<TextView>(R.id.pointsText)

        if (usernameText == null || avatarImage == null || btnSendRequest == null) {
            Log.e("ProfileActivity", "Friend search result has missing required views")
            showToast(getString(R.string.error_generic))
            return
        }

        usernameText.text = user.username
        pointsText.text = getString(R.string.profile_points_format, user.points)
        avatarManager.loadAvatar(user.id.toString(), avatarImage, user.avatarUrl)

        btnSendRequest.setOnClickListener {
            sendFriendRequest(user.id, btnSendRequest)
        }

        container.addView(view)
    }

    private fun sendFriendRequest(toUserId: UUID, button: Button) {
        if (toUserId == currentProfileId) {
            showToast(getString(R.string.cannot_add_yourself))
            return
        }
        val accessToken = tokenManager.getAccessToken()
        if (accessToken == null) {
            showToast(getString(R.string.session_expired))
            return
        }

        lifecycleScope.launch {
            try {
                val request = RelationshipRequest(toUserId = toUserId)
                val response = ApiClient.relationshipService.sendFriendRequest(
                    "Bearer $accessToken",
                    request
                )

                if (response.isSuccessful) {
                    showToast(getString(R.string.friend_request_sent))
                    button.isEnabled = false
                    button.text = getString(R.string.request_sent)
                    RefreshEvents.notifyChanged(RefreshEvents.DataSet.FRIENDS)
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast(UserMessage.fromServerText(this@ProfileActivity, errorBody))
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Friend request failed", e)
                showToast(UserMessage.from(this@ProfileActivity, e))
            }
        }
    }

    private fun resetAll() {
        binding.testTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.mainTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.cupTest.setTextColor(Color.parseColor("#7A7A7B"))
        binding.profileTest.setTextColor(Color.parseColor("#7A7A7B"))

        binding.mainIcon.setColorFilter(Color.parseColor("#7A7A7B"))

        binding.testIcon.setImageResource(R.drawable.graduation24)
        binding.mainIcon.setImageResource(R.drawable.swords24)
        binding.cupIcon.setImageResource(R.drawable.trophy24)
        binding.profileIcon.setImageResource(R.drawable.profile24)
    }
}
