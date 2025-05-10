package com.example.duelingo.activity

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.duelingo.R
import com.example.duelingo.adapters.FriendsAdapter
import com.example.duelingo.databinding.ActivityMenuBinding
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.request.RelationshipRequest
import com.example.duelingo.dto.response.FriendResponse
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.UserService
import com.example.duelingo.network.websocket.DuelWebSocketClient
import com.example.duelingo.network.websocket.StompManager
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null
    private lateinit var tokenManager: TokenManager
    private lateinit var avatarManager: AvatarManager
    private lateinit var userService: UserService
    private lateinit var stompManager: StompManager
    private var loadingDialog: ProgressDialog? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        Log.d("MenuActivity", "onCreate started")
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("MenuActivity", "Initializing AvatarManager")
        avatarManager = AvatarManager(this, tokenManager, getSharedPreferences("user_prefs", MODE_PRIVATE))

        Log.d("MenuActivity", "Creating Retrofit client")
        val retrofit = RetrofitClient.getClient(tokenManager)
        userService = retrofit.create(UserService::class.java)

        binding.friendsContainer.apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
        }
        Log.d("MenuActivity", "Loading friends list")
        loadFriends()

        binding.mainIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.mainTest.setTextColor(Color.parseColor("#FF00A5FE"))


        Log.d("MenuActivity", "Creating DuelWebSocketClient")
        stompManager = StompManager(tokenManager)
        setupDuelButton()

        binding.btnCancelSearch.setOnClickListener {
            Log.d("MenuActivity", "Cancel search button clicked")
            scope.launch { cancelDuelSearch() }
        }

        binding.addFriendButton.setOnClickListener {
            Log.d("MenuActivity", "Add friend button clicked")
            showAddFriendDialog()
        }

        Log.d("MenuActivity", "Setting up navigation buttons")
        setupNavigationButtons()
        Log.d("MenuActivity", "onCreate completed")
    }

    override fun onDestroy() {
        Log.d("MenuActivity", "onDestroy started")
        scope.cancel()
        Log.d("MenuActivity", "Disconnecting WebSocket")
        stompManager.disconnect()
        loadingDialog?.dismiss()
        super.onDestroy()
        Log.d("MenuActivity", "onDestroy completed")
    }

    private fun setupDuelButton() {
        Log.d("MenuActivity", "setupDuelButton started")
        binding.btnDuel.setOnClickListener {
            Log.d("MenuActivity", "Duel button clicked, current text: ${binding.btnDuel.text}")
            scope.launch {
                if (binding.btnDuel.text == "DUEL") {
                    Log.d("MenuActivity", "Starting duel search")
                    startDuelSearch()
                } else {
                    Log.d("MenuActivity", "Canceling duel search")
                    cancelDuelSearch()
                }
            }
        }
        Log.d("MenuActivity", "setupDuelButton completed")
    }

    private suspend fun startDuelSearch() {
        Log.d("MenuActivity", "startDuelSearch started")
        withContext(Dispatchers.Main) {
            binding.btnDuel.text = "CANCEL"
            Log.d("MenuActivity", "Changed duel button text to CANCEL")
            showLoading(true)
        }

        try {
            Log.d("MenuActivity", "Checking WebSocket connection")
            if (!stompManager.isConnected()) {
                Log.d("MenuActivity", "WebSocket not connected, connecting...")
                connectToWebSocket()
                delay(500)
            }

            if (!stompManager.isConnected()) {
                throw IllegalStateException("WebSocket connection failed")
            }
            Log.d("MenuActivity", "WebSocket connected: ${stompManager.isConnected()}")
            joinMatchmakingQueue()

            withContext(Dispatchers.Main) {
                Log.d("MenuActivity", "Showing searching toast")
                Toast.makeText(this@MenuActivity, "Searching for opponent...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MenuActivity", "Error in startDuelSearch: ${e.message}", e)
            withContext(Dispatchers.Main) {
                loadingDialog?.dismiss()
                Toast.makeText(
                    this@MenuActivity,
                    "Error: ${e.message ?: "Failed to start duel"}",
                    Toast.LENGTH_LONG
                ).show()
                Log.d("MenuActivity", "Calling cancelDuelSearch after error")
                cancelDuelSearch()
            }
        }
        Log.d("MenuActivity", "startDuelSearch completed")
    }


    private suspend fun cancelDuelSearch() {
        Log.d("MenuActivity", "cancelDuelSearch started")
        withContext(Dispatchers.Main) {
            binding.btnDuel.text = "DUEL"
            Log.d("MenuActivity", "Changed duel button text to DUEL")
            showLoading(false)
        }

        try {
            Log.d("MenuActivity", "Canceling matchmaking")
            stompManager.cancelMatchmaking()
            Log.d("MenuActivity", "Triggering vibration")
            vibrate(50)
        } catch (e: Exception) {
            Log.e("MenuActivity", "Error in cancelDuelSearch", e)
        } finally {
            Log.d("MenuActivity", "Disconnecting WebSocket")
            stompManager.disconnect()
        }
        Log.d("MenuActivity", "cancelDuelSearch completed")
    }


    private suspend fun connectToWebSocket() {
        Log.d("MenuActivity", "connectToWebSocket started")
        try {
            Log.d("MenuActivity", "Starting WebSocket connection")
            suspendCancellableCoroutine<Unit> { continuation ->
                Log.d("MenuActivity", "Calling stompManager.connect")
                stompManager.connect(
                    onConnected = {
                        Log.d("MenuActivity", "WebSocket connected successfully")
                        continuation.resume(Unit) { /* обработка отмены */ }
                    },
                    onError = { error ->
                        Log.e("MenuActivity", "WebSocket connection error", error)
                        continuation.resumeWithException(error)
                    },
                    onDuelFound = { duelInfo ->
                        Log.d("MenuActivity", "Duel found with opponent: ${duelInfo.opponentId}")
                        scope.launch { startDuelActivity(duelInfo) }
                    },
                    onMatchmakingFailed = { reason ->
                        Log.d("MenuActivity", "Matchmaking failed: ${reason.reason}")
                        scope.launch {
                            Toast.makeText(
                                this@MenuActivity,
                                "Matchmaking failed: ${reason.reason}",
                                Toast.LENGTH_LONG
                            ).show()
                            cancelDuelSearch()
                        }
                    }
                )

                continuation.invokeOnCancellation {
                    Log.d("MenuActivity", "WebSocket connection cancelled")
                    stompManager.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e("MenuActivity", "Error in connectToWebSocket", e)
            throw IOException("Failed to establish WebSocket connection", e)
        }
        Log.d("MenuActivity", "connectToWebSocket completed")
    }

    private suspend fun joinMatchmakingQueue() {
        Log.d("MenuActivity", "joinMatchmakingQueue started")
        try {
            Log.d("MenuActivity", "Joining matchmaking queue")
            if (!stompManager.joinMatchmaking()) {  // Используем метод joinMatchmaking из StompManager
                throw IllegalStateException("Failed to join matchmaking queue")
            }
            withContext(Dispatchers.Main) {
                Log.d("MenuActivity", "Showing searching toast")
                Toast.makeText(this@MenuActivity, "Searching for opponent...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MenuActivity", "Error in joinMatchmakingQueue", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MenuActivity, "Matchmaking error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d("MenuActivity", "Calling cancelDuelSearch after error")
                cancelDuelSearch()
            }
        }
        Log.d("MenuActivity", "joinMatchmakingQueue completed")
    }


    private fun showLoading(show: Boolean) {
        Log.d("MenuActivity", "showLoading: $show")
        if (show) {
            binding.searchOverlay.bringToFront()
            binding.searchOverlay.visibility = View.VISIBLE
        } else {
            binding.searchOverlay.visibility = View.GONE
        }
    }
    private fun vibrate(durationMs: Long) {
        Log.d("MenuActivity", "vibrate: $durationMs ms")
        (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.let {
            if (Build.VERSION.SDK_INT >= 26) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                it.vibrate(durationMs)
            }
        }
    }
    private fun startDuelActivity(duelInfo: DuelFoundEvent) {
        Log.d("MenuActivity", "startDuelActivity started with opponent: ${duelInfo.opponentId}")
        runOnUiThread {
            try {
                loadingDialog?.dismiss()
                Log.d("MenuActivity", "Creating DuelActivity intent")
                val intent = Intent(this, DuelActivity::class.java).apply {
                    putExtra("DUEL_INFO", Gson().toJson(duelInfo))
                }
                Log.d("MenuActivity", "Starting DuelActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("MenuActivity", "Error starting DuelActivity", e)
                Toast.makeText(
                    this,
                    "Duel started! (Debug: ${duelInfo.opponentId})",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        Log.d("MenuActivity", "startDuelActivity completed")
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
                    layoutManager = LinearLayoutManager(this@MenuActivity)
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
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
                icon.visibility = View.VISIBLE
                animationView.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animator) {
            }

        })
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
    private fun setupNavigationButtons() {
        Log.d("MenuActivity", "setupNavigationButtons started")
        binding.tests.setOnClickListener {
            Log.d("MenuActivity", "Tests button clicked")
            resetAll()
            startActivity(Intent(this, LearningActivity::class.java))
            changeColorAndIcon(binding.testIcon, binding.testTest, R.drawable.grad)
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
        }

        binding.leaderboard.setOnClickListener {
            Log.d("MenuActivity", "Leaderboard button clicked")
            resetAll()
            startActivity(Intent(this, RankActivity::class.java))
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }

        binding.profile.setOnClickListener {
            Log.d("MenuActivity", "Profile button clicked")
            resetAll()
            startActivity(Intent(this, ProfileActivity::class.java))
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(binding.profAnimation, binding.profileIcon, binding.profileTest, "profAnim.json")
        }
        Log.d("MenuActivity", "setupNavigationButtons completed")
    }
    object RetrofitClient {
        fun getClient(tokenManager: TokenManager): Retrofit {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer ${tokenManager.getAccessToken()}")
                        .build()
                    chain.proceed(request)
                }
                .connectTimeout(AppConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(AppConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(AppConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(AppConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
}