package com.example.duelingo.activity

import android.animation.Animator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.duelingo.R
import com.example.duelingo.adapters.DuelHistoryAdapter
import com.example.duelingo.databinding.ActivityMenuBinding
import com.example.duelingo.databinding.DialogDuelDifficultyBinding
import com.example.duelingo.databinding.DialogDuelChallengeBinding
import com.example.duelingo.dto.event.DuelFoundEvent
import com.example.duelingo.dto.event.DuelResultEvent
import com.example.duelingo.dto.event.MatchmakingFailedEvent
import com.example.duelingo.dto.event.DuelChallengeEvent
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.DuelHistoryService
import com.example.duelingo.network.UserService
import com.example.duelingo.network.websocket.StompManager
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.storage.OfflineDuelHistoryStore
import com.google.gson.Gson
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.duelingo.utils.OfflineDuelFactory
import com.example.duelingo.utils.LeaderboardCache
import com.example.duelingo.utils.UserMessage
import com.example.duelingo.utils.openTopLevel
import com.example.duelingo.utils.DuelCache
import com.example.duelingo.utils.ConnectivityRetry
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resumeWithException

class MenuActivity : AppCompatActivity() {
    companion object {
        private const val STATE_SEARCHING_FOR_DUEL = "searching_for_duel"
        const val EXTRA_OPEN_LATEST_HISTORY = "open_latest_duel_history"
        const val EXTRA_OPEN_HISTORY_ID = "open_duel_history_id"
    }
    private lateinit var binding: ActivityMenuBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null
    private lateinit var tokenManager: TokenManager
    private lateinit var avatarManager: AvatarManager
    private lateinit var userService: UserService
    private lateinit var duelHistoryService: DuelHistoryService
    private lateinit var stompManager: StompManager
    private lateinit var historyAdapter: DuelHistoryAdapter
    private var currentPage = 0
    private var isLoading = false
    private var hasMorePages = true
    private var isSearchingForDuel = false
    private var historyRefreshJob: Job? = null
    private val pageSize = 5
    private var selectedDifficulty = "MEDIUM"
    private val handledChallenges = mutableSetOf<String>()
    private val startedDuelIds = mutableSetOf<String>()
    private var socketConnectionInProgress = false
    private var contentReady = false
    private var openLatestHistoryRequested = false
    private var requestedHistoryId: String? = null
    private var historyOpenRetryCount = 0

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        Log.d("MenuActivity", "onCreate started")
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        openLatestHistoryRequested = intent.getBooleanExtra(EXTRA_OPEN_LATEST_HISTORY, false)
        requestedHistoryId = intent.getStringExtra(EXTRA_OPEN_HISTORY_ID)
        ConnectivityRetry(this, lifecycle) {
            if (contentReady) {
                refreshDuelHistory()
                loadDuelStats()
            }
        }
        binding.duelHeroImage.setImageDrawable(null)
        binding.duelHeroImage.setBackgroundResource(R.drawable.bg_feature_icon)
        binding.duelHeroImage.post {
            Glide.with(this).load(R.drawable.duel_hero).dontAnimate().centerCrop().into(binding.duelHeroImage)
        }
        Log.d("MenuActivity", "Initializing AvatarManager")
        avatarManager = AvatarManager(this, tokenManager, getSharedPreferences("user_prefs", MODE_PRIVATE))

        Log.d("MenuActivity", "Using shared authenticated API client")
        userService = ApiClient.userService
        duelHistoryService = ApiClient.duelHistoryService

        binding.mainIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.mainTest.setTextColor(Color.parseColor("#FF00A5FE"))

        Log.d("MenuActivity", "Creating DuelWebSocketClient")
        stompManager = StompManager(tokenManager)
        setupDuelButton()
        binding.btnOfflineDuel.setOnClickListener { showDifficultyDialog(offline = true) }

        binding.btnCancelSearch.setOnClickListener {
            Log.d("MenuActivity", "Cancel search button clicked")
            scope.launch { cancelDuelSearch() }
        }

        Log.d("MenuActivity", "Setting up navigation buttons")
        setupNavigationButtons()
        binding.root.post {
            if (isFinishing || isDestroyed) return@post
            setupRecyclerView()
            contentReady = true
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeContent()
        }
        // Matchmaking is always an explicit user action. Restoring it after a
        // configuration change could unexpectedly enqueue a new duel when the
        // user returns from the result screen.
        isSearchingForDuel = false
        binding.btnDuel.setText(R.string.start_duel_search)
        showLoading(false)
        Log.d("MenuActivity", "onCreate completed")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SEARCHING_FOR_DUEL, isSearchingForDuel)
    }

    override fun onDestroy() {
        Log.d("MenuActivity", "onDestroy started")
        scope.cancel()
        Log.d("MenuActivity", "Disconnecting WebSocket")
        stompManager.disconnect()
        super.onDestroy()
        Log.d("MenuActivity", "onDestroy completed")
    }

    override fun onResume() {
        super.onResume()
        if (contentReady) resumeContent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openLatestHistoryRequested = intent.getBooleanExtra(EXTRA_OPEN_LATEST_HISTORY, false)
        requestedHistoryId = intent.getStringExtra(EXTRA_OPEN_HISTORY_ID)
        historyOpenRetryCount = 0
        if (contentReady && openLatestHistoryRequested) refreshDuelHistory()
    }

    private fun resumeContent() {
        LeaderboardCache.prefetch(this, tokenManager.getAccessToken())
        refreshDuelHistory()
        loadDuelStats()
        scope.launch {
            if (!stompManager.isConnected() && !socketConnectionInProgress) {
                socketConnectionInProgress = true
                runCatching { connectToWebSocket() }
                socketConnectionInProgress = false
            }
            loadPendingChallenges()
        }
        historyRefreshJob?.cancel()
        historyRefreshJob = scope.launch {
            while (isActive) {
                delay(45_000)
                refreshDuelHistory()
            }
        }
    }

    override fun onPause() {
        historyRefreshJob?.cancel()
        historyRefreshJob = null
        super.onPause()
    }

    private fun setupDuelButton() {
        Log.d("MenuActivity", "setupDuelButton started")
        binding.btnDuel.setOnClickListener {
            Log.d("MenuActivity", "Duel button clicked, searching: $isSearchingForDuel")
            scope.launch {
                if (isSearchingForDuel) {
                    Log.d("MenuActivity", "Canceling duel search")
                    cancelDuelSearch()
                } else {
                    Log.d("MenuActivity", "Starting duel search")
                    showDifficultyDialog()
                }
            }
        }
        Log.d("MenuActivity", "setupDuelButton completed")
    }

    private fun showDifficultyDialog(offline: Boolean = false) {
        val content = DialogDuelDifficultyBinding.inflate(layoutInflater)
        if (offline) {
            content.titleText.setText(R.string.choose_practice_level)
            content.subtitleText.setText(R.string.offline_practice_subtitle)
        }
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(content.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                val width = (resources.displayMetrics.widthPixels - 32 * resources.displayMetrics.density).toInt()
                    .coerceAtMost((520 * resources.displayMetrics.density).toInt())
                window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }
        fun choose(mode: String) {
            selectedDifficulty = mode
            dialog.dismiss()
            if (offline) {
                val duel = OfflineDuelFactory.create(mode)
                startActivity(Intent(this, DuelActivity::class.java).apply {
                    putExtra("DUEL_INFO", Gson().toJson(duel))
                    putExtra(DuelActivity.EXTRA_OFFLINE_DUEL, true)
                })
            } else {
                scope.launch { startDuelSearch() }
            }
        }
        content.closeButton.setOnClickListener { dialog.dismiss() }
        content.easyButton.setOnClickListener { choose("EASY") }
        content.mediumButton.setOnClickListener { choose("MEDIUM") }
        content.hardButton.setOnClickListener { choose("HARD") }
        dialog.show()
        if (!offline) scope.launch {
            val modes = listOf(
                Triple("EASY", R.string.duel_easy_description, content.easyButton),
                Triple("MEDIUM", R.string.duel_medium_description, content.mediumButton),
                Triple("HARD", R.string.duel_hard_description, content.hardButton)
            )
            modes.map { (mode, label, button) ->
                async {
                    runCatching { duelHistoryService.getMatchmakingEstimate(mode) }
                        .onSuccess { estimate ->
                            button.text = if (estimate.playersWaiting > 0) {
                                getString(R.string.duel_mode_opponent_waiting, getString(label))
                            } else {
                                getString(R.string.duel_mode_average_wait, getString(label), estimate.averageWaitSeconds)
                            }
                        }
                }
            }.forEach { it.await() }
        }
    }

    private suspend fun startDuelSearch() {
        Log.d("MenuActivity", "startDuelSearch started")
        withContext(Dispatchers.Main) {
            isSearchingForDuel = true
            binding.btnDuel.text = getString(R.string.cancel_duel_search)
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
                Toast.makeText(this@MenuActivity, R.string.searching_for_opponent, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MenuActivity", "Error in startDuelSearch: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MenuActivity,
                    UserMessage.from(this@MenuActivity, e),
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
            isSearchingForDuel = false
            binding.btnDuel.text = getString(R.string.start_duel_search)
            Log.d("MenuActivity", "Restored duel search button text")
            showLoading(false)
        }

        try {
            Log.d("MenuActivity", "Canceling matchmaking")
            stompManager.cancelMatchmaking()
            Log.d("MenuActivity", "Triggering vibration")
            vibrate(50)
        } catch (e: Exception) {
            Log.e("MenuActivity", "Error in cancelDuelSearch", e)
        }
        Log.d("MenuActivity", "cancelDuelSearch completed")
    }


    private suspend fun connectToWebSocket() = withContext(Dispatchers.IO) {
        Log.d("MenuActivity", "connectToWebSocket started")
        try {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrEmpty()) {
                throw IllegalStateException("User not authenticated")
            }
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
                        if (continuation.isActive) continuation.resumeWithException(error)
                    },
                    onDuelFound = { duelInfo ->
                        Log.d("MenuActivity", "Duel found with opponent: ${duelInfo.opponentId}")
                        // Matchmaking events are accepted only while the user is
                        // visibly searching. A delayed event from an old queue
                        // must never start a new duel after returning to menu.
                        if (isSearchingForDuel || duelInfo.friendChallenge) {
                            scope.launch { startDuelActivity(duelInfo) }
                        } else {
                            Log.w("MenuActivity", "Ignoring stale duel event ${duelInfo.duel.id}")
                            stompManager.cancelMatchmaking()
                        }
                    },
                    onMatchmakingFailed = { reason ->
                        Log.d("MenuActivity", "Matchmaking failed: ${reason.reason}")
                        scope.launch {
                            Toast.makeText(
                                this@MenuActivity,
                                getString(R.string.error_matchmaking_failed),
                                Toast.LENGTH_LONG
                            ).show()
                            cancelDuelSearch()
                        }
                    },
                    onDuelResult = { result ->
                        Log.d("MenuActivity", "Received duel result in menu (unexpected): $result")
                    },
                    onDuelChallenge = { challenge -> scope.launch { showDuelChallenge(challenge) } }
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
            if (!stompManager.joinMatchmaking(selectedDifficulty)) {
                throw IllegalStateException("Failed to join matchmaking queue")
            }
            withContext(Dispatchers.Main) {
                Log.d("MenuActivity", "Showing searching toast")
                Toast.makeText(this@MenuActivity, R.string.searching_for_opponent, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MenuActivity", "Error in joinMatchmakingQueue", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MenuActivity, UserMessage.from(this@MenuActivity, e), Toast.LENGTH_SHORT).show()
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
        if (!startedDuelIds.add(duelInfo.duel.id)) return
        Log.d("MenuActivity", "startDuelActivity started with opponent: ${duelInfo.opponentId}")
        runOnUiThread {
            try {
                isSearchingForDuel = false
                binding.btnDuel.setText(R.string.start_duel_search)
                showLoading(false)
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
            openTopLevel(LearningActivity::class.java)
            changeColorAndIcon(binding.testIcon, binding.testTest, R.drawable.grad)
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
        }

        binding.leaderboard.setOnClickListener {
            Log.d("MenuActivity", "Leaderboard button clicked")
            resetAll()
            openTopLevel(RankActivity::class.java)
            changeColorAndIcon(binding.cupIcon, binding.cupTest, R.drawable.tro)
            playAnimation(binding.cupAnimation, binding.cupIcon, binding.cupTest, "cupAnim.json")
        }

        binding.profile.setOnClickListener {
            Log.d("MenuActivity", "Profile button clicked")
            resetAll()
            openTopLevel(ProfileActivity::class.java)
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(binding.profAnimation, binding.profileIcon, binding.profileTest, "profAnim.json")
        }
        Log.d("MenuActivity", "setupNavigationButtons completed")
    }

    private fun setupRecyclerView() {
        binding.duelHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        historyAdapter = DuelHistoryAdapter(mutableListOf(), avatarManager) { duel ->
            startActivity(Intent(this, DuelHistoryDetailsActivity::class.java).apply {
                putExtra(DuelHistoryDetailsActivity.EXTRA_DUEL, Gson().toJson(duel))
            })
        }
        binding.duelHistoryRecyclerView.adapter = historyAdapter
        renderHistory(DuelCache.readHistory(this, tokenManager.getAccessToken()))
        binding.duelHistoryRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && hasMorePages) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadDuelHistory()
                    }
                }
            }
        })
    }

    private fun loadDuelHistory() {
        if (isLoading || !hasMorePages) return
        
        isLoading = true
        scope.launch {
            try {
                val response = duelHistoryService.getUserDuelHistory(currentPage, pageSize)
                Log.d("MenuActivity", "Loaded page $currentPage with ${response.content.size} duels")
                
                withContext(Dispatchers.Main) {
                    if (currentPage == 0) {
                        tokenManager.getAccessToken()?.let {
                            DuelCache.storeHistory(this@MenuActivity, it, response.content)
                        }
                        renderHistory(response.content)
                    } else {
                        historyAdapter.addItems(response.content)
                    }
                    
                    hasMorePages = currentPage < response.totalPages - 1
                    currentPage = response.currentPage + 1
                }
            } catch (e: Exception) {
                Log.e("MenuActivity", "Error loading duel history", e)
                if (currentPage == 0) {
                    renderHistory(DuelCache.readHistory(this@MenuActivity, tokenManager.getAccessToken()))
                    Snackbar.make(
                        binding.root,
                        R.string.offline_showing_saved_data,
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.retry_connection) { refreshDuelHistory() }.show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    private fun renderHistory(serverHistory: List<com.example.duelingo.dto.response.DuelInHistoryResponse>) {
        val combinedHistory = OfflineDuelHistoryStore(this, tokenManager.getAccessToken()).getAll() + serverHistory
        historyAdapter.replaceItems(combinedHistory)
        binding.emptyHistoryContainer.visibility = if (combinedHistory.isEmpty()) View.VISIBLE else View.GONE
        binding.duelHistoryRecyclerView.visibility = if (combinedHistory.isEmpty()) View.GONE else View.VISIBLE
        if (openLatestHistoryRequested && combinedHistory.isNotEmpty()) {
            val requested = if (requestedHistoryId == null) {
                combinedHistory.firstOrNull()
            } else {
                combinedHistory.firstOrNull { it.id.toString() == requestedHistoryId }
            }
            if (requested != null) {
                openLatestHistoryRequested = false
                requestedHistoryId = null
                historyOpenRetryCount = 0
                startActivity(Intent(this, DuelHistoryDetailsActivity::class.java).apply {
                    putExtra(DuelHistoryDetailsActivity.EXTRA_DUEL, Gson().toJson(requested))
                })
            } else if (historyOpenRetryCount < 3) {
                historyOpenRetryCount++
                binding.root.postDelayed({
                    if (!isFinishing && openLatestHistoryRequested) refreshDuelHistory()
                }, 800L)
            }
        }
    }

    private suspend fun loadPendingChallenges() {
        runCatching { duelHistoryService.getPendingChallenges() }
            .getOrDefault(emptyList())
            .firstOrNull { it.challengeId !in handledChallenges }
            ?.let { showDuelChallenge(it) }
    }

    private fun showDuelChallenge(challenge: DuelChallengeEvent) {
        if (!handledChallenges.add(challenge.challengeId) || isFinishing) return
        val level = when (challenge.difficulty) {
            "EASY" -> getString(R.string.duel_easy_short)
            "HARD" -> getString(R.string.duel_hard_short)
            else -> getString(R.string.duel_medium_short)
        }
        val content = DialogDuelChallengeBinding.inflate(layoutInflater)
        content.challengerName.text = challenge.challengerUsername
        content.challengeLevel.text = getString(R.string.duel_challenge_level_format, level)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(content.root)
            .setCancelable(false)
            .create()

        content.declineChallengeButton.setOnClickListener {
            dialog.dismiss()
            scope.launch { runCatching { duelHistoryService.respondToChallenge(challenge.challengeId, false) } }
        }
        content.acceptChallengeButton.setOnClickListener {
            content.acceptChallengeButton.isEnabled = false
            content.acceptChallengeButton.text = getString(R.string.duel_challenge_accepting)
            scope.launch {
                runCatching { duelHistoryService.respondToChallenge(challenge.challengeId, true) }
                    .onSuccess { response ->
                        response.body()?.let {
                            dialog.dismiss()
                            startDuelActivity(it)
                        } ?: run {
                            content.acceptChallengeButton.isEnabled = true
                            content.acceptChallengeButton.setText(R.string.accept_duel_challenge)
                            Toast.makeText(this@MenuActivity, R.string.duel_challenge_expired, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure {
                        content.acceptChallengeButton.isEnabled = true
                        content.acceptChallengeButton.setText(R.string.accept_duel_challenge)
                        Toast.makeText(this@MenuActivity, R.string.duel_challenge_expired, Toast.LENGTH_SHORT).show()
                    }
            }
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        vibrate(90)
    }

    private fun refreshDuelHistory() {
        if (isLoading) return
        currentPage = 0
        hasMorePages = true
        loadDuelHistory()
    }

    private fun loadDuelStats() {
        DuelCache.readStats(this, tokenManager.getAccessToken())?.let(::renderDuelStats)
        scope.launch {
            runCatching { duelHistoryService.getUserDuelStats() }
            .onSuccess { stats ->
                renderDuelStats(stats)
                tokenManager.getAccessToken()?.let { DuelCache.storeStats(this@MenuActivity, it, stats) }
            }.onFailure { error ->
                Log.w("MenuActivity", "Could not load duel statistics", error)
            }
        }
    }

    private fun renderDuelStats(stats: com.example.duelingo.dto.response.DuelStatsResponse) {
        binding.duelsPlayedValue.text = stats.total.toString()
        binding.duelsWonValue.text = stats.wins.toString()
        binding.duelWinRateValue.text = "${stats.winRate}%"
    }

}
