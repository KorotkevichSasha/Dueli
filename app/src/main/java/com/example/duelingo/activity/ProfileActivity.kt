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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.adapters.FriendRequestsAdapter
import com.example.duelingo.adapters.FriendsAdapter
import com.example.duelingo.databinding.ActivityProfileBinding
import com.example.duelingo.dto.request.RelationshipRequest
import com.example.duelingo.dto.response.FriendResponse
import com.example.duelingo.dto.response.UserProfileResponse
import com.example.duelingo.fragment.FriendRequestsFragment
import com.example.duelingo.fragment.FriendsListFragment
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.manager.ThemeManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.UserService
import com.example.duelingo.storage.TokenManager
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
    private val sharedPreferences by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }

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

        binding.profileIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.profileTest.setTextColor(Color.parseColor("#FF00A5FE"))

        avatarManager = AvatarManager(this, tokenManager, sharedPreferences)
        binding.profileImage.setOnClickListener { getContent.launch("image/*") }
        loadProfile()

        binding.achievementsButton.setOnClickListener{ startActivity(Intent(this@ProfileActivity, AchievementActivity::class.java)) }

        setupFriendsSection()

        binding.tests.setOnClickListener {
            resetAll()
            startActivity(Intent(this@ProfileActivity, LearningActivity::class.java))
            changeColorAndIcon(binding.testIcon, binding.testTest, R.drawable.grad)
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
        }
        binding.duel.setOnClickListener {
            resetAll()
            startActivity(Intent(this@ProfileActivity, MenuActivity::class.java))
            changeColorAndIcon(binding.mainIcon, binding.mainTest, R.drawable.swo)
            playAnimation(binding.duelAnimation, binding.mainIcon, binding.mainTest, "swordAnim.json")
        }
        binding.leaderboard.setOnClickListener {
            resetAll()
            startActivity(Intent(this@ProfileActivity, RankActivity::class.java))
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
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> FriendsListFragment.newInstance()
                    1 -> FriendRequestsFragment.newInstance()
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
                0 -> "Друзья"
                1 -> "Заявки"
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

        val logoutButton = dialog.findViewById<LinearLayout>(R.id.logout_button)
        logoutButton.setOnClickListener {
            dialog.dismiss()
            logout()
        }

        dialog.show()
    }

    private fun updateUI(response: UserProfileResponse) {
        binding.playerName.text = response.username
        binding.playerEmail.text = response.email
        binding.pointCount.text = "Очки: ${response.points}"
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
        val accessToken = tokenManager.getAccessToken() ?: run {
            showToast("Access token is missing.")
            return
        }

        val tokenWithBearer = "Bearer $accessToken"

        lifecycleScope.launch {
            try {
                val response = ApiClient.userService.getProfile(tokenWithBearer)
                withContext(Dispatchers.Main) {
                    updateUI(response)
                }
            } catch (e: Exception) {
                Log.e("ProfileError", "Error loading profile: ${e.message}")
                showToast("Error loading profile. Please try again.")
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
        dialog.setContentView(R.layout.dialog_add_friend)

        val rootView = dialog.findViewById<ViewGroup>(android.R.id.content)
        if (rootView == null) {
            showToast("Error: Failed to load dialog layout")
            return
        }

        val editUsername = rootView.findViewById<EditText>(R.id.editUsername)
        val btnSearch = rootView.findViewById<Button>(R.id.btnSearch)
        val progressBar = rootView.findViewById<ProgressBar>(R.id.progressBar)
        val userContainer = rootView.findViewById<LinearLayout>(R.id.userContainer)

        if (editUsername == null || btnSearch == null || progressBar == null || userContainer == null) {
            showToast("Error: Dialog layout is incorrect")
            return
        }

        btnSearch.setOnClickListener {
            val username = editUsername.text.toString()
            if (username.isNotEmpty()) {
                searchUser(username, progressBar, userContainer)
            } else {
                showToast("Please enter username")
            }
        }

        dialog.show()
    }

    private fun searchUser(username: String, progressBar: ProgressBar, container: LinearLayout) {
        if (!::tokenManager.isInitialized) {
            showToast("Error: TokenManager is not initialized")
            return
        }

        val accessToken = tokenManager.getAccessToken()
        if (accessToken == null) {
            showToast("Error: Access token is missing")
            return
        }

        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                container.removeAllViews()

                val response = ApiClient.userService.searchUsers(
                    "Bearer $accessToken",
                    username
                )

                if (response.content.isNotEmpty()) {
                    response.content.forEach { user ->
                        showUserInfo(user, container)
                    }
                } else {
                    showToast("No users found")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
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

        if (usernameText == null || avatarImage == null || btnSendRequest == null) {
            showToast("Error: User item layout is incorrect")
            return
        }

        usernameText.text = user.username
        avatarManager.loadAvatar(user.id.toString(), avatarImage)

        btnSendRequest.setOnClickListener {
            sendFriendRequest(user.id)
        }

        container.addView(view)
    }

    private fun sendFriendRequest(toUserId: UUID) {
        val accessToken = tokenManager.getAccessToken()
        if (accessToken == null) {
            showToast("Error: Access token is missing")
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
                    showToast("Friend request sent!")
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Error: ${errorBody ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                showToast("Error sending request: ${e.message}")
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