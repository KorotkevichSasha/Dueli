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
import kotlinx.coroutines.launch

class RankActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRankBinding
    private var currentAnimationView: LottieAnimationView? = null
    private var currentIcon: ImageView? = null
    private var currentText: TextView? = null

    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var leaderboardRecyclerView: RecyclerView

    private lateinit var avatarManager: AvatarManager
    private val tokenManager by lazy { TokenManager(this) }
    private val sharedPreferences by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankBinding.inflate(layoutInflater)
        setContentView(binding.root)

        avatarManager = AvatarManager(this, tokenManager, sharedPreferences)

        binding.cupIcon.setColorFilter(Color.parseColor("#FF00A5FE"))
        binding.cupTest.setTextColor(Color.parseColor("#FF00A5FE"))

        leaderboardRecyclerView = findViewById(R.id.rvLeaderboard)
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        leaderboardAdapter = LeaderboardAdapter(createEmptyLeaderboardResponse(), avatarManager)

        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        leaderboardRecyclerView.adapter = leaderboardAdapter
        loadLeaderboard()

        binding.tests.setOnClickListener {
            resetAll();
            startActivity(Intent(this@RankActivity, LearningActivity::class.java))
            changeColorAndIcon(
                binding.testIcon,
                binding.testTest,
                R.drawable.grad
            )
            playAnimation(binding.testAnimation, binding.testIcon, binding.testTest, "graAnim.json")
        }
        binding.duel.setOnClickListener {
            resetAll();
            startActivity(Intent(this@RankActivity, MenuActivity::class.java))
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
            startActivity(Intent(this@RankActivity, ProfileActivity::class.java))
            changeColorAndIcon(binding.profileIcon, binding.profileTest, R.drawable.prof)
            playAnimation(
                binding.profAnimation,
                binding.profileIcon,
                binding.profileTest,
                "profAnim.json"
            )
        }
    }

    private fun createEmptyLeaderboardResponse(): LeaderboardResponse {
        val emptyPaginationResponse = PaginationResponse<UserInLeaderboardResponse>(
            content = emptyList(),
            totalItems = 0,
            totalPages = 0,
            currentPage = 0
        )
        val emptyUser = UserInLeaderboardResponse("", "", 0, "", 0)
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
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val tokenWithBearer = "Bearer $accessToken"

            lifecycleScope.launch {
                try {
                    val response = ApiClient.leaderboardService.getLeaderboard(tokenWithBearer)
                    leaderboardAdapter.updateData(response)
                    updateUI(response)
                } catch (e: Exception) {
                    showToast(e.toString())

                }
            }
        } else {
            showToast("RankActivity" + "Access token is missing.")
        }
    }
    private fun updateUI(response: LeaderboardResponse) {
        val currentUser = response.currentUser
        if (currentUser != null) {
            updateCurrentUserInfo(currentUser)
        }

        val adapter = LeaderboardAdapter(response, avatarManager)
        binding.rvLeaderboard.adapter = adapter
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
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