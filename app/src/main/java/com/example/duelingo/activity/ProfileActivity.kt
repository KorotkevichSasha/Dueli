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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.duelingo.R
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.adapters.FriendRequestsAdapter
import com.example.duelingo.adapters.FriendsAdapter
import com.example.duelingo.databinding.ActivityProfileBinding
import com.example.duelingo.dto.request.RelationshipRequest
import com.example.duelingo.dto.response.FriendResponse
import com.example.duelingo.dto.response.UserProfileResponse
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.manager.ThemeManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.UserService
import com.example.duelingo.storage.TokenManager
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

        binding.friendsContainer.apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
        }

        // Initialize friends list after all necessary services are set up
        Log.d("ProfileActivity", "Loading friends list")
        loadFriends()

        binding.addFriendButton.setOnClickListener {
            showAddFriendDialog()
        }

        binding.friendRequestsButton.setOnClickListener {
            showFriendRequestsDialog()
        }

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

    private fun loadFriends() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val friends = userService.getCurrentUserFriends("Bearer ${tokenManager.getAccessToken()}")
                    .distinctBy { it.id }

                binding.friendsTitle.text = if (friends.isNotEmpty()) {
                    "Ваши друзья (${friends.size})"
                } else {
                    "У вас пока нет друзей"
                }

                Log.d("FriendsDebug", "Received friends: ${friends.map { it.username }}")

                binding.friendsRecyclerView.apply {
                    layoutManager = LinearLayoutManager(this@ProfileActivity)
                    adapter = FriendsAdapter(friends, avatarManager)

                    addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                        ContextCompat.getDrawable(context, R.drawable.divider)?.let { drawable ->
                            setDrawable(drawable)
                        }
                    })
                }
            } catch (e: Exception) {
                binding.friendsTitle.text = "Ошибка загрузки друзей"
                Log.e("MenuActivity", "Error loading friends", e)
            }
        }
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

        Log.d("DialogDebug", "editUsername: ${editUsername != null}")
        Log.d("DialogDebug", "btnSearch: ${btnSearch != null}")
        Log.d("DialogDebug", "progressBar: ${progressBar != null}")
        Log.d("DialogDebug", "userContainer: ${userContainer != null}")

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
        Log.d("MenuActivity", "Starting search for username: $username")

        val accessToken = tokenManager.getAccessToken()
        if (accessToken == null) {
            Log.e("MenuActivity", "Access token is missing")
            showToast("Error: Access token is missing")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("MenuActivity", "Making API request...")
                progressBar.visibility = View.VISIBLE
                container.removeAllViews()

                val response = ApiClient.userService.searchUsers(
                    "Bearer $accessToken",
                    username
                )

                Log.d("MenuActivity", "API response received: ${response.content.size} users found")

                if (response.content.isNotEmpty()) {
                    response.content.forEach { user ->
                        Log.d("MenuActivity", "Showing user: ${user.username}")
                        showUserInfo(user, container)
                    }
                } else {
                    Log.d("MenuActivity", "No users found")
                    showToast("No users found")
                }
            } catch (e: Exception) {
                Log.e("MenuActivity", "Error during search: ${e.message}", e)
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

        if (usernameText == null ||  avatarImage == null || btnSendRequest == null) {
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

    private fun showFriendRequestsDialog() {
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog)
        dialog.setContentView(R.layout.dialog_add_friend)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.requestsRecyclerView)

        val adapter = FriendRequestsAdapter(
            avatarManager = avatarManager,
            onAccept = { requestId -> updateRequestStatus(requestId, "accept", recyclerView) },
            onReject = { requestId -> updateRequestStatus(requestId, "reject", recyclerView) }
        )

        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = adapter

        loadFriendRequests(adapter)
        dialog.show()
    }

    private fun updateRequestStatus(requestId: UUID, action: String, recyclerView: RecyclerView?) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.relationshipService.updateRelationshipStatus(
                    "Bearer ${tokenManager.getAccessToken()}",
                    requestId,
                    action
                )

                if (response.isSuccessful) {
                    showToast("Request updated")
                    val adapter = (recyclerView?.adapter as? FriendRequestsAdapter)
                    adapter?.let { loadFriendRequests(it) }
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Error: ${errorBody ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                showToast("Error updating request: ${e.message}")
            }
        }
    }

    private fun loadFriendRequests(adapter: FriendRequestsAdapter) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.relationshipService.getIncomingRequests(
                    "Bearer ${tokenManager.getAccessToken()}"
                )

                if (response.isSuccessful) {
                    response.body()?.let { requests ->
                        adapter.submitList(requests)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Error: ${errorBody ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                showToast("Error loading requests: ${e.message}")
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