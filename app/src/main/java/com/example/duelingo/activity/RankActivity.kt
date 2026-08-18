package com.example.duelingo.activity

import android.animation.Animator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.duelingo.R
import com.example.duelingo.adapters.LeaderboardAdapter
import com.example.duelingo.databinding.ActivityRankBinding
import com.example.duelingo.dto.response.LeaderboardResponse
import com.example.duelingo.dto.response.PaginationResponse
import com.example.duelingo.dto.response.UserInLeaderboardResponse
import com.example.duelingo.manager.AvatarManager
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.LeaderboardCache
import com.example.duelingo.utils.UserMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.duelingo.utils.openTopLevel
import com.example.duelingo.utils.ConnectivityRetry
import com.google.android.material.snackbar.Snackbar

class RankActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRankBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null

    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var leaderboardRecyclerView: RecyclerView

    private lateinit var avatarManager: AvatarManager
    private var refreshJob: Job? = null
    private var leaderboardLoading = false
    private var offlineSnackbar: Snackbar? = null
    private var leaderboardRendered = false
    private val tokenManager by lazy { TokenManager(this) }
    private val sharedPreferences by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankBinding.inflate(layoutInflater)
        setContentView(binding.root)

        avatarManager = AvatarManager(this, tokenManager, sharedPreferences)

        binding.cupIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.cupTest.setTextColor(Color.parseColor("#FF00A5FE"))

        ConnectivityRetry(this, lifecycle) { loadLeaderboard() }
        val cachedLeaderboard = LeaderboardCache.current(this, tokenManager.getAccessToken())
        leaderboardRecyclerView = binding.rvLeaderboard
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        leaderboardAdapter = LeaderboardAdapter(
            cachedLeaderboard ?: createEmptyLeaderboardResponse(),
            avatarManager
        )
        leaderboardRecyclerView.adapter = leaderboardAdapter
        if (cachedLeaderboard != null) {
            updateUI(cachedLeaderboard)
        } else {
            binding.leaderboardLoading.visibility = View.VISIBLE
            binding.userContainer.visibility = View.INVISIBLE
        }

        binding.tests.setOnClickListener {
            resetAll();
            openTopLevel(LearningActivity::class.java)
            changeColorAndIcon(
                binding.testIcon,
                binding.testTest,
                R.drawable.grad
            )
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
        }
        binding.duel.setOnClickListener {
            resetAll();
            openTopLevel(MenuActivity::class.java)
            changeColorAndIcon(binding.mainIcon, binding.mainTest, R.drawable.swo)
            playAnimation(
                binding.duelAnimation,
                binding.mainIcon,
                binding.mainTest,
                "swordAnim.json"
            )
        }
        binding.leaderboard.setOnClickListener {

        }
        binding.profile.setOnClickListener {
            resetAll();
            openTopLevel(ProfileActivity::class.java)
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(
                binding.profAnimation,
                binding.profileIcon,
                binding.profileTest,
                "profAnim.json"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadLeaderboard()
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(45_000)
                loadLeaderboard()
            }
        }
    }

    override fun onPause() {
        refreshJob?.cancel()
        refreshJob = null
        super.onPause()
    }

    private fun createEmptyLeaderboardResponse(): LeaderboardResponse {
        val emptyPaginationResponse = PaginationResponse<UserInLeaderboardResponse>(
            content = emptyList(),
            totalItems = 0,
            totalPages = 0,
            currentPage = 0
        )
        val emptyUser = UserInLeaderboardResponse("", "", 0, "", 0, null)
        return LeaderboardResponse(emptyPaginationResponse, emptyUser)
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

            private fun playAnimation(animationFile: String) {
                binding.animationView.setAnimation(animationFile)
                binding.animationView.playAnimation()
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
    private fun loadLeaderboard() {
        if (leaderboardLoading) return
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val tokenWithBearer = "Bearer $accessToken"

            lifecycleScope.launch {
                leaderboardLoading = true
                try {
                    val response = ApiClient.leaderboardService.getLeaderboard(tokenWithBearer)
                    LeaderboardCache.store(this@RankActivity, accessToken, response)
                    updateUI(response)
                    offlineSnackbar?.dismiss()
                    offlineSnackbar = null
                } catch (e: Exception) {
                    val cached = LeaderboardCache.current(this@RankActivity, accessToken)
                    if (cached != null) updateUI(cached)
                    offlineSnackbar?.dismiss()
                    offlineSnackbar = Snackbar.make(
                        binding.root,
                        R.string.offline_showing_saved_data,
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction(R.string.retry_connection) { loadLeaderboard() }
                    offlineSnackbar?.show()
                } finally {
                    leaderboardLoading = false
                }
            }
        } else {
            showToast(getString(R.string.session_expired))
        }
    }
    private fun updateUI(response: LeaderboardResponse) {
        binding.leaderboardLoading.visibility = View.GONE
        binding.userContainer.visibility = View.VISIBLE
        val currentUser = response.currentUser
        updateCurrentUserInfo(currentUser)
        binding.tvRankSummary.text = "#${currentUser.rank}"
        binding.tvPointsSummary.text = currentUser.points.toString()
        binding.tvPlayersSummary.text = response.top.totalItems.toString()

        binding.tvRankProgress.text = if (currentUser.pointsToNextRank == null) {
            getString(R.string.rank_first_place)
        } else {
            getString(R.string.rank_next_format, currentUser.pointsToNextRank)
        }

        findViewById<TextView?>(R.id.rankGapIndicator)?.apply {
            val lastVisibleRank = response.top.content.lastOrNull()?.rank ?: 0
            val missing = currentUser.rank - lastVisibleRank - 1
            visibility = if (missing > 0) View.VISIBLE else View.GONE
            text = resources.getQuantityString(
                R.plurals.rank_hidden_positions,
                missing.toInt(),
                missing.toInt()
            )
        }

        leaderboardAdapter.updateData(response)
        if (!leaderboardRendered) {
            binding.leagueSummaryCard.apply {
                alpha = 0f
                scaleX = 0.98f
                scaleY = 0.98f
                animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
            }
            leaderboardRendered = true
        }
    }
    private fun updateCurrentUserInfo(currentUser: UserInLeaderboardResponse) {
        binding.tvUserRank.text = currentUser.rank.toString()
        binding.tvUsername.text = currentUser.username
        binding.tvUserPoints.text = currentUser.points.toString()

        avatarManager.loadAvatar(currentUser.id, binding.ivUserAvatar)
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
